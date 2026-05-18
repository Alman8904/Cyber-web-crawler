package com.supply.supplyTrace.alert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.supply.supplyTrace.analyzer.RiskFinding;
import com.supply.supplyTrace.site.MonitoredSite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private static final long DEDUPLICATION_WINDOW_MS = 24 * 60 * 60 * 1000; // 24 hours

    public List<AlertResponse> createAlerts(MonitoredSite site, List<RiskFinding> findings) {
        if (findings == null || findings.isEmpty()) {
            return List.of();
        }

        Instant now = Instant.now();
        Instant deduplicationCutoff = now.minusMillis(DEDUPLICATION_WINDOW_MS);

        List<Alert> alertsToCreate = new ArrayList<>();

        for (RiskFinding finding : findings) {
            // Check if this exact alert was created in the last 24 hours
            long recentCount = alertRepository.countRecentAlert(site, finding.type(), finding.message(), deduplicationCutoff);

            // Only create the alert if it doesn't already exist recently
            if (recentCount == 0) {
                alertsToCreate.add(Alert.builder()
                        .site(site)
                        .severity(finding.severity())
                        .type(finding.type())
                        .message(finding.message())
                        .createdAt(now)
                        .build());
            }
        }

        if (alertsToCreate.isEmpty()) {
            return List.of();
        }

        List<Alert> saved = alertRepository.saveAll(alertsToCreate);
        return saved.stream().map(this::toResponse).toList();
    }

    public AlertResponse createSystemAlert(MonitoredSite site, AlertSeverity severity, AlertType type, String message) {
        Alert alert = alertRepository.save(Alert.builder()
                .site(site)
                .severity(severity)
                .type(type)
                .message(message)
                .createdAt(Instant.now())
                .build());
        return toResponse(alert);
    }

    public AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getSite() == null ? null : alert.getSite().getId(),
                alert.getSeverity(),
                alert.getType(),
                alert.getMessage(),
                alert.getCreatedAt()
        );
    }
}

