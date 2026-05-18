package com.supply.supplyTrace.scheduler;

import com.supply.supplyTrace.site.MonitoredSiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SiteScanScheduler {

    private final MonitoredSiteService monitoredSiteService;

    @Scheduled(
            fixedDelayString = "${supplytrace.scheduling.scan-delay-ms:86400000}",
            initialDelayString = "${supplytrace.scheduling.scan-initial-delay-ms:60000}"
    )
    public void scanSites() {
        log.info("Starting scheduled SupplyTrace scan");
        monitoredSiteService.scanAllSites();
        log.info("Finished scheduled SupplyTrace scan");
    }
}

