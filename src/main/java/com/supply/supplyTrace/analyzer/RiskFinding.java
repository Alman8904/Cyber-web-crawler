package com.supply.supplyTrace.analyzer;

import com.supply.supplyTrace.alert.AlertSeverity;
import com.supply.supplyTrace.alert.AlertType;

public record RiskFinding(
        AlertSeverity severity,
        AlertType type,
        String message,
        int scoreContribution
) {
}

