package com.supply.supplyTrace.analyzer;

import java.util.List;

public record SnapshotDiffResponse(
        List<String> newScripts,
        List<String> removedScripts,
        List<String> newStylesheets,
        List<String> removedStylesheets,
        List<String> newIframes,
        List<String> removedIframes,
        List<String> newPlugins,
        List<String> removedPlugins,
        List<String> newDomains,
        List<String> removedDomains,
        boolean initialScan,
        boolean hasChanges
) {
}


