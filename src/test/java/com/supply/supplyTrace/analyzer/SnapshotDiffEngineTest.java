package com.supply.supplyTrace.analyzer;

import java.util.List;

import com.supply.supplyTrace.snapshot.SiteSnapshotPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotDiffEngineTest {

    private final SnapshotDiffEngine diffEngine = new SnapshotDiffEngine();

    @Test
    void detectsAddedAndRemovedResources() {
        SiteSnapshotPayload previous = new SiteSnapshotPayload(
                List.of("https://cdn.example.com/old.js"),
                List.of("https://cdn.example.com/style-old.css"),
                List.of(),
                List.of(),
                List.of("cdn.example.com")
        );

        SiteSnapshotPayload latest = new SiteSnapshotPayload(
                List.of("https://cdn.example.com/new.js"),
                List.of("https://cdn.example.com/style-old.css", "https://cdn.example.com/style-new.css"),
                List.of("https://widgets.example.com/frame"),
                List.of(),
                List.of("cdn.example.com", "widgets.example.com")
        );

        SnapshotDiffResult result = diffEngine.compare(previous, latest);

        assertThat(result.initialScan()).isFalse();
        assertThat(result.newScripts()).containsExactly("https://cdn.example.com/new.js");
        assertThat(result.removedScripts()).containsExactly("https://cdn.example.com/old.js");
        assertThat(result.newStylesheets()).containsExactly("https://cdn.example.com/style-new.css");
        assertThat(result.newIframes()).containsExactly("https://widgets.example.com/frame");
        assertThat(result.newDomains()).containsExactly("widgets.example.com");
    }
}

