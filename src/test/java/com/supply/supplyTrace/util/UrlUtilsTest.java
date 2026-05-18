package com.supply.supplyTrace.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlUtilsTest {

    @Test
    void normalizesAbsoluteAndRelativeUrls() {
        assertThat(UrlUtils.normalizeUrl("HTTP://Example.com", null)).isEqualTo("http://example.com/");
        assertThat(UrlUtils.normalizeUrl("/js/app.js", "https://example.com/page"))
                .isEqualTo("https://example.com/js/app.js");
    }

    @Test
    void detectsSamePartyAndTrustedDomains() {
        assertThat(UrlUtils.isSameParty("cdn.example.com", "www.example.com")).isTrue();
        assertThat(UrlUtils.isTrustedDomain("cdnjs.cloudflare.com")).isTrue();
    }
}

