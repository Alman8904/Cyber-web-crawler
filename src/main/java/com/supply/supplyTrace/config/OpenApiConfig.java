package com.supply.supplyTrace.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI supplyTraceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SupplyTrace API")
                        .description("Monitor website supply-chain integrity: crawl dependencies, compare snapshots, score risk, and raise alerts.")
                        .version("1.0"));
    }
}
