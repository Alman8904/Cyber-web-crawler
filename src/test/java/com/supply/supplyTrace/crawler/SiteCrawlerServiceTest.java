package com.supply.supplyTrace.crawler;

import static org.assertj.core.api.Assertions.assertThat;


import com.supply.supplyTrace.snapshot.SiteSnapshotPayload;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

class SiteCrawlerServiceTest {

    private final SiteCrawlerService siteCrawlerService = new SiteCrawlerService(new OkHttpClient());

    @Test
    void extractsScriptsStylesIframesPluginsAndDomains() {
        String html = """
                <html>
                  <head>
                    <link rel="stylesheet" href="/css/app.css">
                    <script src="https://cdn.example.com/lib.js"></script>
                    <script src="/wp-content/plugins/contact-form.js"></script>
                  </head>
                  <body>
                    <iframe src="https://widgets.example.org/embed"></iframe>
                    <a href="https://tracker.example.net/plugin/widget.js">Link</a>
                  </body>
                </html>
                """;

        SiteSnapshotPayload payload = siteCrawlerService.extractSnapshot(html, "https://example.com/index.html");

        assertThat(payload.scripts()).containsExactly("https://cdn.example.com/lib.js", "https://example.com/wp-content/plugins/contact-form.js");
        assertThat(payload.stylesheets()).containsExactly("https://example.com/css/app.css");
        assertThat(payload.iframes()).containsExactly("https://widgets.example.org/embed");
        assertThat(payload.plugins()).contains("https://example.com/wp-content/plugins/contact-form.js", "https://tracker.example.net/plugin/widget.js");
        assertThat(payload.domains()).containsExactlyInAnyOrder("widgets.example.org", "tracker.example.net");
    }
}

