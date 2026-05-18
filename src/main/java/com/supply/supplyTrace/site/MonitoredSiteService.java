package com.supply.supplyTrace.site;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supply.supplyTrace.alert.AlertRepository;
import com.supply.supplyTrace.alert.AlertResponse;
import com.supply.supplyTrace.alert.AlertService;
import com.supply.supplyTrace.alert.AlertSeverity;
import com.supply.supplyTrace.alert.AlertType;
import com.supply.supplyTrace.analyzer.RiskAssessment;
import com.supply.supplyTrace.analyzer.RiskScoringService;
import com.supply.supplyTrace.analyzer.SnapshotDiffEngine;
import com.supply.supplyTrace.analyzer.SnapshotDiffResponse;
import com.supply.supplyTrace.crawler.CrawlResult;
import com.supply.supplyTrace.crawler.SiteCrawlException;
import com.supply.supplyTrace.crawler.SiteCrawlerService;
import com.supply.supplyTrace.snapshot.SiteSnapshot;
import com.supply.supplyTrace.snapshot.SiteSnapshotPayload;
import com.supply.supplyTrace.snapshot.SiteSnapshotRepository;
import com.supply.supplyTrace.snapshot.SnapshotComparisonResponse;
import com.supply.supplyTrace.util.HashUtils;
import com.supply.supplyTrace.util.UrlUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonitoredSiteService {

    private final MonitoredSiteRepository siteRepository;
    private final SiteSnapshotRepository snapshotRepository;
    private final AlertRepository alertRepository;
    private final SiteCrawlerService crawlerService;
    private final SnapshotDiffEngine diffEngine;
    private final RiskScoringService riskScoringService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    public SiteResponse createSite(CreateSiteRequest request) {
        String normalizedUrl = UrlUtils.normalizeUrl(request.url(), null);
        if (normalizedUrl == null) {
            throw new IllegalArgumentException("Only http and https URLs are supported: " + request.url());
        }

        MonitoredSite site = siteRepository.findByUrl(normalizedUrl)
                .orElseGet(() -> siteRepository.save(MonitoredSite.builder()
                        .url(normalizedUrl)
                        .riskScore(0)
                        .build()));
        return toResponse(site);
    }

    public List<SiteResponse> listSites() {
        return siteRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SiteResponse getSite(Long id) {
        return toResponse(findSite(id));
    }

    @Transactional
    public void deleteSite(Long id) {
        MonitoredSite site = findSite(id);
        alertRepository.deleteBySite(site);
        snapshotRepository.deleteBySite(site);
        siteRepository.delete(site);
    }

    public List<AlertResponse> getAlerts(Long siteId) {
        MonitoredSite site = findSite(siteId);
        return alertRepository.findBySiteOrderByCreatedAtDesc(site)
                .stream()
                .map(alertService::toResponse)
                .toList();
    }

    public DashboardResponse getDashboard() {
        return new DashboardResponse(
                siteRepository.count(),
                snapshotRepository.count(),
                alertRepository.countBySeverity(AlertSeverity.HIGH),
                alertRepository.findTop10ByOrderByCreatedAtDesc()
                        .stream()
                        .map(alertService::toResponse)
                        .toList(),
                Instant.now()
        );
    }

    public EnrichedDashboardResponse getEnrichedDashboard() {
        List<MonitoredSite> allSites = siteRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        List<EnrichedDashboardResponse.SiteMetrics> siteMetrics = allSites.stream()
                .map(site -> {
                    long snapshotCount = snapshotRepository.countBySite(site);
                    long alertCount = alertRepository.countBySite(site);
                    long highRiskAlerts = alertRepository.countBySiteAndSeverity(site, AlertSeverity.HIGH);

                    SiteSnapshotPayload latest = getLatestSnapshot(site);
                    int uniqueDomainsCount = 0;
                    int uniqueScriptsCount = 0;
                    
                    if (latest != null) {
                        if (latest.domains() != null) {
                            uniqueDomainsCount = latest.domains().size();
                        }
                        if (latest.scripts() != null) {
                            uniqueScriptsCount = latest.scripts().size();
                        }
                    }

                    int recentChanges = 0;
                    List<SiteSnapshot> recentSnapshots = snapshotRepository.findBySiteOrderByScannedAtDesc(site);
                    if (recentSnapshots.size() >= 2) {
                        try {
                            SiteSnapshotPayload previous = objectMapper.readValue(recentSnapshots.get(1).getSnapshotJson(), SiteSnapshotPayload.class);
                            var diff = diffEngine.compare(previous, latest);
                            if (diff != null) {
                                recentChanges = (
                                        (diff.newScripts() == null ? 0 : diff.newScripts().size()) +
                                        (diff.removedScripts() == null ? 0 : diff.removedScripts().size()) +
                                        (diff.newDomains() == null ? 0 : diff.newDomains().size()) +
                                        (diff.removedDomains() == null ? 0 : diff.removedDomains().size())
                                );
                            }
                        } catch (IOException ex) {
                            recentChanges = 0;
                        }
                    }

                    return new EnrichedDashboardResponse.SiteMetrics(
                            site.getId(),
                            site.getUrl(),
                            site.getRiskScore(),
                            site.getLastScan(),
                            snapshotCount,
                            alertCount,
                            highRiskAlerts,
                            uniqueDomainsCount,
                            uniqueScriptsCount,
                            recentChanges
                    );
                })
                .collect(Collectors.toList());

        // Calculate total unique domains across all sites
        int totalUniqueDomainsCount = allSites.stream()
                .map(this::getLatestSnapshot)
                .filter(snap -> snap != null && snap.domains() != null && !snap.domains().isEmpty())
                .flatMap(snap -> snap.domains().stream())
                .collect(Collectors.toSet())
                .size();

        return new EnrichedDashboardResponse(
                siteRepository.count(),
                snapshotRepository.count(),
                totalUniqueDomainsCount,
                alertRepository.countBySeverity(AlertSeverity.HIGH),
                alertRepository.countBySeverity(AlertSeverity.MEDIUM),
                alertRepository.countBySeverity(AlertSeverity.LOW),
                siteMetrics,
                alertRepository.findTop10ByOrderByCreatedAtDesc()
                        .stream()
                        .map(alertService::toResponse)
                        .toList(),
                Instant.now()
        );
    }

    public SnapshotComparisonResponse getSnapshotComparison(Long siteId) {
        MonitoredSite site = findSite(siteId);
        List<SiteSnapshot> snapshots = snapshotRepository.findBySiteOrderByScannedAtDesc(site);

        if (snapshots.isEmpty()) {
            throw new NotFoundException("No snapshots found for site: " + siteId);
        }

        SiteSnapshot currentSnapshot = snapshots.get(0);
        SiteSnapshot previousSnapshot = snapshots.size() > 1 ? snapshots.get(1) : null;

        try {
            SiteSnapshotPayload currentPayload = objectMapper.readValue(currentSnapshot.getSnapshotJson(), SiteSnapshotPayload.class);
            SiteSnapshotPayload previousPayload = previousSnapshot == null ? null :
                    objectMapper.readValue(previousSnapshot.getSnapshotJson(), SiteSnapshotPayload.class);

            var diff = diffEngine.compare(previousPayload, currentPayload);

            SnapshotComparisonResponse.SnapshotData currentData = new SnapshotComparisonResponse.SnapshotData(
                    currentSnapshot.getId(),
                    currentSnapshot.getScannedAt(),
                    currentSnapshot.getHash(),
                    currentPayload.scripts() == null ? 0 : currentPayload.scripts().size(),
                    currentPayload.stylesheets() == null ? 0 : currentPayload.stylesheets().size(),
                    currentPayload.iframes() == null ? 0 : currentPayload.iframes().size(),
                    currentPayload.plugins() == null ? 0 : currentPayload.plugins().size(),
                    currentPayload.domains() == null ? 0 : currentPayload.domains().size(),
                    currentPayload.scripts() == null ? List.of() : currentPayload.scripts(),
                    currentPayload.stylesheets() == null ? List.of() : currentPayload.stylesheets(),
                    currentPayload.iframes() == null ? List.of() : currentPayload.iframes(),
                    currentPayload.plugins() == null ? List.of() : currentPayload.plugins(),
                    currentPayload.domains() == null ? List.of() : currentPayload.domains()
            );

            SnapshotComparisonResponse.SnapshotData previousData = null;
            if (previousPayload != null) {
                previousData = new SnapshotComparisonResponse.SnapshotData(
                        previousSnapshot.getId(),
                        previousSnapshot.getScannedAt(),
                        previousSnapshot.getHash(),
                        previousPayload.scripts() == null ? 0 : previousPayload.scripts().size(),
                        previousPayload.stylesheets() == null ? 0 : previousPayload.stylesheets().size(),
                        previousPayload.iframes() == null ? 0 : previousPayload.iframes().size(),
                        previousPayload.plugins() == null ? 0 : previousPayload.plugins().size(),
                        previousPayload.domains() == null ? 0 : previousPayload.domains().size(),
                        previousPayload.scripts() == null ? List.of() : previousPayload.scripts(),
                        previousPayload.stylesheets() == null ? List.of() : previousPayload.stylesheets(),
                        previousPayload.iframes() == null ? List.of() : previousPayload.iframes(),
                        previousPayload.plugins() == null ? List.of() : previousPayload.plugins(),
                        previousPayload.domains() == null ? List.of() : previousPayload.domains()
                );
            }

            return new SnapshotComparisonResponse(
                    site.getId(),
                    site.getUrl(),
                    previousData,
                    currentData,
                    diff == null ? createEmptyDiff() : toDiffResponse(diff)
            );
        } catch (IOException ex) {
            throw new RuntimeException("Failed to parse snapshots: " + ex.getMessage(), ex);
        }
    }

    private SiteSnapshotPayload getLatestSnapshot(MonitoredSite site) {
        return snapshotRepository.findTopBySiteOrderByScannedAtDesc(site)
                .map(snapshot -> {
                    try {
                        return objectMapper.readValue(snapshot.getSnapshotJson(), SiteSnapshotPayload.class);
                    } catch (IOException ex) {
                        return null;
                    }
                })
                .orElse(null);
    }

    @Transactional
    public ScanResponse scanSite(Long siteId) {
        MonitoredSite site = findSite(siteId);
        Instant scannedAt = Instant.now();

        try {
            CrawlResult crawlResult = crawlerService.crawl(site.getUrl());
            SiteSnapshotPayload latestPayload = crawlResult.payload();
            String snapshotJson = objectMapper.writeValueAsString(latestPayload);
            String snapshotHash = HashUtils.sha256(snapshotJson);

            SiteSnapshot previousSnapshot = snapshotRepository.findTopBySiteOrderByScannedAtDesc(site).orElse(null);
            SiteSnapshotPayload previousPayload = previousSnapshot == null
                    ? null
                    : objectMapper.readValue(previousSnapshot.getSnapshotJson(), SiteSnapshotPayload.class);

            SiteSnapshot savedSnapshot = snapshotRepository.save(SiteSnapshot.builder()
                    .site(site)
                    .snapshotJson(snapshotJson)
                    .hash(snapshotHash)
                    .scannedAt(scannedAt)
                    .build());

            var diff = diffEngine.compare(previousPayload, latestPayload);
            RiskAssessment assessment = riskScoringService.assess(site.getUrl(), latestPayload, diff);
            site.setRiskScore(assessment.score());
            site.setLastScan(scannedAt);
            siteRepository.save(site);

            List<AlertResponse> createdAlerts = alertService.createAlerts(site, assessment.findings());
            return new ScanResponse(
                    site.getId(),
                    site.getUrl(),
                    true,
                    site.getRiskScore(),
                    scannedAt,
                    savedSnapshot.getHash(),
                    toDiffResponse(diff),
                    createdAlerts,
                    "Scan completed successfully"
            );
        } catch (SiteCrawlException | IOException ex) {
            site.setRiskScore(100);
            site.setLastScan(scannedAt);
            siteRepository.save(site);
            AlertResponse alert = alertService.createSystemAlert(
                    site,
                    AlertSeverity.HIGH,
                    AlertType.SITE_UNREACHABLE,
                    "Failed to scan site: " + ex.getMessage()
            );
            return new ScanResponse(
                    site.getId(),
                    site.getUrl(),
                    false,
                    site.getRiskScore(),
                    scannedAt,
                    null,
                    null,
                    List.of(alert),
                    ex.getMessage()
            );
        }
    }

    public List<ScanResponse> scanAllSites() {
        return siteRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(site -> scanSite(site.getId()))
                .toList();
    }

    private MonitoredSite findSite(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Monitored site not found: " + id));
    }

    private SiteResponse toResponse(MonitoredSite site) {
        return new SiteResponse(site.getId(), site.getUrl(), site.getRiskScore(), site.getLastScan());
    }

    private SnapshotDiffResponse toDiffResponse(com.supply.supplyTrace.analyzer.SnapshotDiffResult diff) {
        if (diff == null) {
            return createEmptyDiff();
        }
        return new SnapshotDiffResponse(
                diff.newScripts(),
                diff.removedScripts(),
                diff.newStylesheets(),
                diff.removedStylesheets(),
                diff.newIframes(),
                diff.removedIframes(),
                diff.newPlugins(),
                diff.removedPlugins(),
                diff.newDomains(),
                diff.removedDomains(),
                diff.initialScan(),
                diff.hasChanges()
        );
    }

    private SnapshotDiffResponse createEmptyDiff() {
        return new SnapshotDiffResponse(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                false
        );
    }
}

