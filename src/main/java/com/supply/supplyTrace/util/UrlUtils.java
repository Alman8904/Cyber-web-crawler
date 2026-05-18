package com.supply.supplyTrace.util;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class UrlUtils {

    private static final Set<String> TRUSTED_DOMAINS = Set.of(
            // CDN Providers
            "cdnjs.cloudflare.com",
            "cdn.jsdelivr.net",
            "jsdelivr.net",
            "unpkg.com",
            "fonts.googleapis.com",
            "fonts.gstatic.com",
            "ajax.googleapis.com",
            "code.jquery.com",
            "stackpath.bootstrapcdn.com",

            // Google Analytics & Tag Manager
            "google-analytics.com",
            "googletagmanager.com",
            "www.googletagmanager.com",
            "analytics.google.com",
            "www.google-analytics.com",

            // Facebook & Meta
            "facebook.com",
            "facebook.net",
            "www.facebook.com",
            "connect.facebook.net",
            "platform.facebook.com",

            // Twitter/X
            "twitter.com",
            "x.com",
            "platform.twitter.com",
            "analytics.twitter.com",

            // GitHub
            "github.com",
            "www.github.com",
            "github.githubassets.com",
            "raw.githubusercontent.com",
            "user-images.githubusercontent.com",

            // Cloudflare
            "cloudflare.com",
            "www.cloudflare.com",
            "cdn.cloudflare.net",

            // AWS/CloudFront
            "cloudfront.net",
            "amazonaws.com",
            "s3.amazonaws.com",

            // Akamai
            "akamai.net",
            "akamaitech.net",

            // LinkedIn
            "linkedin.com",
            "www.linkedin.com",
            "platform.linkedin.com",

            // Stripe & Payment
            "stripe.com",
            "js.stripe.com",
            "checkout.stripe.com",

            // Mixpanel
            "mixpanel.com",
            "api.mixpanel.com",

            // Segment
            "segment.com",
            "cdn.segment.com",

            // Intercom
            "intercom.com",
            "widget.intercom.io",

            // HubSpot
            "hubspot.com",
            "js.hubspot.com",
            "cdn2.hubspot.net",

            // Hotjar
            "hotjar.com",
            "script.hotjar.com",

            // Amplitude
            "amplitude.com",
            "api.amplitude.com",

            // Sentry
            "sentry.io",
            "cdn.ravenjs.com",

            // DataDog
            "datadoghq.com",
            "cdn-datadoghq.com",
            "www.datadogcdn.com",

            // New Relic
            "newrelic.com",
            "bam.nr-data.net",

            // Brightcove (Video)
            "brightcove.net",
            "bcvcdn.net",

            // Vimeo
            "vimeo.com",
            "player.vimeo.com",

            // YouTube
            "youtube.com",
            "www.youtube.com",
            "youtube-nocookie.com",
            "yt.be",

            // Typekit/Adobe
            "typekit.net",
            "use.typekit.net",

            // Font Awesome
            "fontawesome.com",
            "ka-f.fontawesome.com",

            // Microsoft
            "microsoft.com",
            "edge.microsoft.com",
            "msecnd.net",

            // Apple
            "apple.com",
            "appleid.cdn-apple.com",

            // WordPress
            "wordpress.com",
            "wp.com",
            "jetpack.wordpress.com",

            // Google APIs
            "googleapis.com",
            "gstatic.com",
            "google.com",

            // Bootstrap & Popular Libraries
            "bootstrapcdn.com",
            "maxcdn.bootstrapcdn.com",

            // jQuery
            "jquery.com",

            // Matomo
            "matomo.org",

            // Piwik
            "piwik.org",

            // AdSense & Google Ads
            "adsense.google.com",
            "pagead2.googlesyndication.com",
            "googleadservices.com",

            // Doubleclick
            "doubleclick.net",

            // OpenX
            "openx.net",

            // Chartbeat
            "chartbeat.net",

            // Vendor Services
            "vendor.com",
            "service.com"
    );

    private UrlUtils() {
    }

    public static String normalizeUrl(String rawUrl, String baseUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String candidateValue = rawUrl.trim();
        try {
            URI base = baseUrl == null || baseUrl.isBlank() ? null : new URI(baseUrl.trim());
            URI candidate = base == null ? new URI(candidateValue) : base.resolve(candidateValue);
            String scheme = candidate.getScheme();
            if (scheme == null) {
                return null;
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return null;
            }

            String host = candidate.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            host = IDN.toASCII(host.toLowerCase(Locale.ROOT));

            int port = candidate.getPort();
            if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
                port = -1;
            }

            String path = candidate.getRawPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }

            URI normalized = new URI(
                    scheme,
                    candidate.getUserInfo(),
                    host,
                    port,
                    path,
                    candidate.getRawQuery(),
                    null
            );
            return normalized.toString();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    public static String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            return IDN.toASCII(host.toLowerCase(Locale.ROOT));
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    public static boolean isTrustedDomain(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        for (String trustedDomain : TRUSTED_DOMAINS) {
            if (normalized.equals(trustedDomain) || normalized.endsWith("." + trustedDomain)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSameParty(String resourceHost, String siteHost) {
        if (resourceHost == null || siteHost == null) {
            return false;
        }
        String left = stripWww(resourceHost.toLowerCase(Locale.ROOT));
        String right = stripWww(siteHost.toLowerCase(Locale.ROOT));
        if (left.equals(right) || left.endsWith("." + right) || right.endsWith("." + left)) {
            return true;
        }
        return rootDomain(left).equals(rootDomain(right));
    }

    public static boolean isIpAddress(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) {
            return true;
        }
        return normalized.matches("^(?:\\d{1,3}\\.){3}\\d{1,3}$");
    }

    public static String rootDomain(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String[] parts = host.toLowerCase(Locale.ROOT).split("\\.");
        if (parts.length <= 2) {
            return host.toLowerCase(Locale.ROOT);
        }
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    public static boolean isPluginPath(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = url.toLowerCase(Locale.ROOT);
        List<String> keywords = List.of("/plugin", "/plugins/", "/widget", "/widgets/", "/extension", "/extensions/", "/addon", "/addons/", "/integration", "/integrations/", "wp-content/plugins");
        for (String keyword : keywords) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String stripWww(String host) {
        if (host.startsWith("www.")) {
            return host.substring(4);
        }
        return host;
    }
}

