package com.supply.supplyTrace.analyzer;

import java.util.List;

public record SnapshotDiffResult(
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
        boolean initialScan
) {

    public boolean hasChanges() {
        return !newScripts.isEmpty()
                || !removedScripts.isEmpty()
                || !newStylesheets.isEmpty()
                || !removedStylesheets.isEmpty()
                || !newIframes.isEmpty()
                || !removedIframes.isEmpty()
                || !newPlugins.isEmpty()
                || !removedPlugins.isEmpty()
                || !newDomains.isEmpty()
                || !removedDomains.isEmpty();
    }
}

