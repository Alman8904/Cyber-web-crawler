package com.supply.supplyTrace.analyzer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.supply.supplyTrace.snapshot.SiteSnapshotPayload;
import org.springframework.stereotype.Service;

@Service
public class SnapshotDiffEngine {

    public SnapshotDiffResult compare(SiteSnapshotPayload previous, SiteSnapshotPayload latest) {
        if (previous == null) {
            // First scan: treat all found items as "new"
            return new SnapshotDiffResult(
                    latest.scripts() == null ? List.of() : latest.scripts(),
                    List.of(),
                    latest.stylesheets() == null ? List.of() : latest.stylesheets(),
                    List.of(),
                    latest.iframes() == null ? List.of() : latest.iframes(),
                    List.of(),
                    latest.plugins() == null ? List.of() : latest.plugins(),
                    List.of(),
                    latest.domains() == null ? List.of() : latest.domains(),
                    List.of(),
                    true
            );
        }

        return new SnapshotDiffResult(
                difference(latest.scripts(), previous.scripts()),
                difference(previous.scripts(), latest.scripts()),
                difference(latest.stylesheets(), previous.stylesheets()),
                difference(previous.stylesheets(), latest.stylesheets()),
                difference(latest.iframes(), previous.iframes()),
                difference(previous.iframes(), latest.iframes()),
                difference(latest.plugins(), previous.plugins()),
                difference(previous.plugins(), latest.plugins()),
                difference(latest.domains(), previous.domains()),
                difference(previous.domains(), latest.domains()),
                false
        );
    }

    private List<String> difference(List<String> left, List<String> right) {
        LinkedHashSet<String> values = new LinkedHashSet<>(left == null ? List.of() : left);
        values.removeAll(new LinkedHashSet<>(right == null ? List.of() : right));
        return new ArrayList<>(values);
    }
}

