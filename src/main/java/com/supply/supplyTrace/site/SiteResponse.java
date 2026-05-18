package com.supply.supplyTrace.site;

import java.time.Instant;

public record SiteResponse(
        Long id,
        String url,
        Integer riskScore,
        Instant lastScan
) {
}





