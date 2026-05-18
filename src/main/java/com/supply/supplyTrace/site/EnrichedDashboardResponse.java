package com.supply.supplyTrace.site;

import java.time.Instant;
import java.util.List;


import com.supply.supplyTrace.alert.AlertResponse;

public record EnrichedDashboardResponse(
        // Overall Metrics
        long totalSites,
        long totalSnapshots,
        long totalDomainsTracked,
        long highRiskAlertsCount,
        long mediumRiskAlertsCount,
        long lowRiskAlertsCount,

        // Per-Site Details
        List<SiteMetrics> sites,

        // Recent Alerts
        List<AlertResponse> latestAlerts,

        // Generated Time
        Instant generatedAt
) {
    public record SiteMetrics(
            Long siteId,
            String url,
            int riskScore,
            Instant lastScan,
            long totalSnapshots,
            long totalAlerts,
            long highRiskAlerts,
            int uniqueDomainsCount,
            int uniqueScriptsCount,
            int recentChanges // number of changes detected in last scan
    ) {
    }
}

