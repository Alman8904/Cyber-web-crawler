package com.supply.supplyTrace.site;

import java.time.Instant;
import java.util.List;

import com.supply.supplyTrace.alert.AlertResponse;
import com.supply.supplyTrace.analyzer.SnapshotDiffResponse;

public record ScanResponse(
        Long siteId,
        String url,
        boolean success,
        Integer riskScore,
        Instant scannedAt,
        String snapshotHash,
        SnapshotDiffResponse diff,
        List<AlertResponse> alertsCreated,
        String message
) {
}





