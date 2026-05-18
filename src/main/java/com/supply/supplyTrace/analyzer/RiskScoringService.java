package com.supply.supplyTrace.analyzer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.supply.supplyTrace.alert.AlertSeverity;
import com.supply.supplyTrace.alert.AlertType;
import com.supply.supplyTrace.snapshot.SiteSnapshotPayload;
import com.supply.supplyTrace.util.UrlUtils;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {

    public RiskAssessment assess(String siteUrl, SiteSnapshotPayload latest, SnapshotDiffResult diff) {
        List<RiskFinding> findings = new ArrayList<>();
        String siteHost = UrlUtils.extractHost(siteUrl);
        boolean secureSite = siteUrl != null && siteUrl.startsWith("https://");

        Set<String> resources = new LinkedHashSet<>();
        resources.addAll(latest.scripts());
        resources.addAll(latest.stylesheets());
        resources.addAll(latest.iframes());
        resources.addAll(latest.plugins());

        for (String resource : resources) {
            addResourceRisk(findings, resource, siteHost, secureSite);
        }

        if (diff != null) {
            addDiffRisk(findings, diff.newScripts(), AlertType.NEW_SCRIPT, 18, "New third-party script detected: %s");
            addDiffRisk(findings, diff.removedScripts(), AlertType.REMOVED_SCRIPT, 8, "Script removed from the page: %s");
            addDiffRisk(findings, diff.newStylesheets(), AlertType.NEW_STYLESHEET, 6, "New stylesheet detected: %s");
            addDiffRisk(findings, diff.removedStylesheets(), AlertType.REMOVED_STYLESHEET, 4, "Stylesheet removed from the page: %s");
            addDiffRisk(findings, diff.newIframes(), AlertType.NEW_IFRAME, 10, "New iframe detected: %s");
            addDiffRisk(findings, diff.removedIframes(), AlertType.REMOVED_IFRAME, 5, "Iframe removed from the page: %s");
            addDiffRisk(findings, diff.newPlugins(), AlertType.NEW_PLUGIN, 12, "Plugin-related path added: %s");
            addDiffRisk(findings, diff.removedPlugins(), AlertType.REMOVED_PLUGIN, 6, "Plugin-related path removed: %s");
            addDiffRisk(findings, diff.newDomains(), AlertType.NEW_DOMAIN, 15, "New third-party domain detected: %s");
            addDiffRisk(findings, diff.removedDomains(), AlertType.REMOVED_DOMAIN, 5, "Third-party domain removed: %s");
        }

        long untrustedDomains = latest.domains() == null
                ? 0
                : latest.domains().stream().filter(domain -> !UrlUtils.isTrustedDomain(domain)).count();

        if (untrustedDomains > 5) {
            findings.add(new RiskFinding(
                    AlertSeverity.MEDIUM,
                    AlertType.TOO_MANY_THIRD_PARTY_DOMAINS,
                    "The site depends on more than five third-party domains.",
                    12
            ));
        }

        int score = findings.stream().mapToInt(RiskFinding::scoreContribution).sum();
        if (score > 100) {
            score = 100;
        }
        return new RiskAssessment(score, List.copyOf(findings));
    }

    private void addResourceRisk(List<RiskFinding> findings, String resource, String siteHost, boolean secureSite) {
        String host = UrlUtils.extractHost(resource);
        if (host == null) {
            return;
        }

        if (UrlUtils.isIpAddress(host)) {
            findings.add(new RiskFinding(
                    AlertSeverity.HIGH,
                    AlertType.RAW_IP_RESOURCE,
                    "Raw IP resource detected: " + resource,
                    25
            ));
        }

        if (secureSite && resource.startsWith("http://")) {
            findings.add(new RiskFinding(
                    AlertSeverity.HIGH,
                    AlertType.HTTP_MIXED_CONTENT,
                    "HTTP resource loaded on an HTTPS site: " + resource,
                    20
            ));
        }

        if (!UrlUtils.isSameParty(host, siteHost) && !UrlUtils.isTrustedDomain(host)) {
            findings.add(new RiskFinding(
                    AlertSeverity.HIGH,
                    AlertType.SUSPICIOUS_DOMAIN,
                    "Suspicious third-party domain found: " + host,
                    15
            ));
        }
    }

    private void addDiffRisk(List<RiskFinding> findings, List<String> items, AlertType type, int score, String messageTemplate) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (String item : items) {
            String host = hostFromItem(item);
            boolean trusted = host != null && UrlUtils.isTrustedDomain(host);
            AlertSeverity severity = severityFor(type, trusted);
            int adjustedScore = scoreFor(type, score, trusted);
            String message = String.format(Locale.ROOT, messageTemplate, item);
            findings.add(new RiskFinding(severity, type, message, adjustedScore));
        }
    }

    private String hostFromItem(String item) {
        String host = UrlUtils.extractHost(item);
        if (host != null) {
            return host;
        }

        if (item == null) {
            return null;
        }

        String normalized = item.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isBlank() && !normalized.contains("://") && !normalized.contains("/") && normalized.contains(".")) {
            return normalized;
        }
        return null;
    }

    private AlertSeverity severityFor(AlertType type, boolean trusted) {
        if (trusted && (type == AlertType.NEW_SCRIPT || type == AlertType.NEW_DOMAIN || type == AlertType.NEW_STYLESHEET || type == AlertType.REMOVED_SCRIPT || type == AlertType.REMOVED_DOMAIN)) {
            return AlertSeverity.LOW;
        }

        return switch (type) {
            case NEW_SCRIPT, NEW_DOMAIN, RAW_IP_RESOURCE, HTTP_MIXED_CONTENT, SUSPICIOUS_DOMAIN, RISK_SCORE_HIGH -> AlertSeverity.HIGH;
            case TOO_MANY_THIRD_PARTY_DOMAINS, NEW_IFRAME, NEW_PLUGIN, REMOVED_SCRIPT, REMOVED_PLUGIN, REMOVED_DOMAIN, REMOVED_STYLESHEET, REMOVED_IFRAME -> AlertSeverity.MEDIUM;
            case NEW_STYLESHEET -> AlertSeverity.LOW;
            default -> AlertSeverity.LOW;
        };
    }

    private int scoreFor(AlertType type, int baseScore, boolean trusted) {
        if (!trusted) {
            return baseScore;
        }

        return switch (type) {
            case NEW_SCRIPT, NEW_DOMAIN -> 2;
            case NEW_STYLESHEET, REMOVED_SCRIPT, REMOVED_DOMAIN -> 1;
            case REMOVED_STYLESHEET -> 1;
            default -> Math.max(1, baseScore / 4);
        };
    }
}

