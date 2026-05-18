package com.supply.supplyTrace.snapshot;

import java.util.List;

public record SiteSnapshotPayload(
        List<String> scripts,
        List<String> stylesheets,
        List<String> iframes,
        List<String> plugins,
        List<String> domains
) {
}

