package com.supply.supplyTrace.crawler;

import com.supply.supplyTrace.snapshot.SiteSnapshotPayload;

public record CrawlResult(
        SiteSnapshotPayload payload,
        String finalUrl,
        int statusCode
) {
}

