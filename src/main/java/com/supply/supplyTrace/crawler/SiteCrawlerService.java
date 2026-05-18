package com.supply.supplyTrace.crawler;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.supply.supplyTrace.snapshot.SiteSnapshotPayload;
import com.supply.supplyTrace.util.UrlUtils;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteCrawlerService {

    private final OkHttpClient okHttpClient;

    @Value("${supplytrace.crawler.user-agent:SupplyTraceBot/1.0}")
    private String userAgent;

    public CrawlResult crawl(String siteUrl) {
        String normalizedSiteUrl = UrlUtils.normalizeUrl(siteUrl, null);
        if (normalizedSiteUrl == null) {
            throw new SiteCrawlException("Only http and https URLs are supported: " + siteUrl);
        }

        Request request = new Request.Builder()
                .url(normalizedSiteUrl)
                .header("User-Agent", userAgent)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                throw new SiteCrawlException("The site returned an empty response body: " + normalizedSiteUrl);
            }
            String html = body.string();
            String finalUrl = response.request().url().toString();
            SiteSnapshotPayload payload = extractSnapshot(html, finalUrl);
            return new CrawlResult(payload, finalUrl, response.code());
        } catch (IOException ex) {
            throw new SiteCrawlException("Failed to crawl site: " + normalizedSiteUrl, ex);
        }
    }

    SiteSnapshotPayload extractSnapshot(String html, String baseUrl) {
        Document document = Jsoup.parse(html == null ? "" : html, baseUrl);
        String siteHost = UrlUtils.extractHost(baseUrl);

        LinkedHashSet<String> scripts = collectNormalized(document.select("script[src]"), "src", baseUrl);
        LinkedHashSet<String> stylesheets = collectNormalized(document.select("link[rel~=stylesheet][href]"), "href", baseUrl);
        LinkedHashSet<String> iframes = collectNormalized(document.select("iframe[src]"), "src", baseUrl);
        LinkedHashSet<String> pluginCandidates = new LinkedHashSet<>();
        pluginCandidates.addAll(collectNormalized(document.select("script[src]"), "src", baseUrl));
        pluginCandidates.addAll(collectNormalized(document.select("iframe[src]"), "src", baseUrl));
        pluginCandidates.addAll(collectNormalized(document.select("link[href]"), "href", baseUrl));
        pluginCandidates.addAll(collectNormalized(document.select("object[data]"), "data", baseUrl));
        pluginCandidates.addAll(collectNormalized(document.select("embed[src]"), "src", baseUrl));
        pluginCandidates.addAll(collectNormalized(document.select("source[src]"), "src", baseUrl));
        pluginCandidates.addAll(collectNormalized(document.select("a[href]"), "href", baseUrl));

        LinkedHashSet<String> plugins = new LinkedHashSet<>();
        LinkedHashSet<String> domains = new LinkedHashSet<>();

        collectDomains(scripts, domains, siteHost);
        collectDomains(stylesheets, domains, siteHost);
        collectDomains(iframes, domains, siteHost);
        collectDomains(pluginCandidates, domains, siteHost);

        for (String resource : pluginCandidates) {
            if (UrlUtils.isPluginPath(resource)) {
                plugins.add(resource);
            }
        }

        return new SiteSnapshotPayload(
                List.copyOf(scripts),
                List.copyOf(stylesheets),
                List.copyOf(iframes),
                List.copyOf(plugins),
                List.copyOf(domains)
        );
    }

    private LinkedHashSet<String> collectNormalized(Elements elements, String attribute, String baseUrl) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        elements.forEach(element -> {
            String absolute = element.absUrl(attribute);
            String normalized = UrlUtils.normalizeUrl(absolute, baseUrl);
            if (normalized != null) {
                urls.add(normalized);
            }
        });
        return urls;
    }

    private void collectDomains(Set<String> resources, Set<String> domains, String siteHost) {
        for (String resource : resources) {
            String host = UrlUtils.extractHost(resource);
            if (host != null && !UrlUtils.isSameParty(host, siteHost)) {
                domains.add(host.toLowerCase(Locale.ROOT));
            }
        }
    }
}

