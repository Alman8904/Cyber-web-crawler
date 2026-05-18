package com.supply.supplyTrace.site;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final MonitoredSiteService monitoredSiteService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return monitoredSiteService.getDashboard();
    }

    @GetMapping("/dashboard/enriched")
    public EnrichedDashboardResponse enrichedDashboard() {
        return monitoredSiteService.getEnrichedDashboard();
    }
}
