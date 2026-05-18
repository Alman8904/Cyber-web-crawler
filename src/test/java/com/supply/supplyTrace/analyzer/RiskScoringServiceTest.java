package com.supply.supplyTrace.analyzer;

import java.util.List;

import com.supply.supplyTrace.alert.AlertSeverity;
import com.supply.supplyTrace.alert.AlertType;
import com.supply.supplyTrace.snapshot.SiteSnapshotPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScoringServiceTest {

    private final RiskScoringService riskScoringService = new RiskScoringService();

    @Test
    void scoresMixedContentRawIpAndSuspiciousDomains() {
        SiteSnapshotPayload snapshot = new SiteSnapshotPayload(
                List.of("http://192.168.1.10/lib.js"),
                List.of(),
                List.of(),
                List.of(),
                List.of("192.168.1.10")
        );
        SnapshotDiffResult diff = new SnapshotDiffResult(
                List.of("http://192.168.1.10/lib.js"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("192.168.1.10"),
                List.of(),
                false
        );

        RiskAssessment assessment = riskScoringService.assess("https://example.com", snapshot, diff);

        assertThat(assessment.score()).isGreaterThanOrEqualTo(40);
        assertThat(assessment.findings())
                .extracting(RiskFinding::type)
                .contains(AlertType.RAW_IP_RESOURCE, AlertType.HTTP_MIXED_CONTENT, AlertType.SUSPICIOUS_DOMAIN);
    }

    @Test
    void keepsTrustedCdnsLowRisk() {
        SiteSnapshotPayload snapshot = new SiteSnapshotPayload(
                List.of("https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js"),
                List.of(),
                List.of(),
                List.of(),
                List.of("cdnjs.cloudflare.com")
        );
        SnapshotDiffResult diff = new SnapshotDiffResult(
                List.of("https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("cdnjs.cloudflare.com"),
                List.of(),
                false
        );

        RiskAssessment assessment = riskScoringService.assess("https://example.com", snapshot, diff);

        assertThat(assessment.score()).isLessThan(10);
        assertThat(assessment.findings()).allSatisfy(finding -> assertThat(finding.severity()).isEqualTo(AlertSeverity.LOW));
    }
}

