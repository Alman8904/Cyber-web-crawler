package com.supply.supplyTrace.snapshot;

import java.time.Instant;
import java.util.List;

import com.supply.supplyTrace.analyzer.SnapshotDiffResponse;

public record SnapshotComparisonResponse(
        Long siteId,
        String siteUrl,

        // Previous Snapshot
        SnapshotData previous,

        // Current Snapshot
        SnapshotData current,

        // Comparison/Diff
        SnapshotDiffResponse changes
) {
    public record SnapshotData(
            Long snapshotId,
            Instant scannedAt,
            String hash,
            int totalScripts,
            int totalStylesheets,
            int totalIframes,
            int totalPlugins,
            int totalDomains,
            List<String> scripts,
            List<String> stylesheets,
            List<String> iframes,
            List<String> plugins,
            List<String> domains
    ) {
    }
}

