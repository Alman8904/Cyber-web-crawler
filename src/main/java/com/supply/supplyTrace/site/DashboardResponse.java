package com.supply.supplyTrace.site;

import java.time.Instant;
import java.util.List;

import com.supply.supplyTrace.alert.AlertResponse;

public record DashboardResponse(
        long totalSites,
        long totalSnapshots,
        long highRiskAlertsCount,
        List<AlertResponse> latestAlerts,
        Instant generatedAt
) {
}


