package com.supply.supplyTrace.analyzer;

import java.util.List;

public record RiskAssessment(
        int score,
        List<RiskFinding> findings
) {
}

