package com.supply.supplyTrace.alert;

import java.time.Instant;

public record AlertResponse(
        Long id,
        Long siteId,
        AlertSeverity severity,
        AlertType type,
        String message,
        Instant createdAt
) {
}


