package com.supply.supplyTrace.config;

import java.time.Duration;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient(@Value("${supplytrace.crawler.timeout-ms:12000}") long timeoutMs) {
        Duration timeout = Duration.ofMillis(timeoutMs);
        return new OkHttpClient.Builder()
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .callTimeout(timeout.plusSeconds(3))
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }
}

