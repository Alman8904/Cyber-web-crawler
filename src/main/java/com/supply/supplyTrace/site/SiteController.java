package com.supply.supplyTrace.site;

import java.util.List;

import com.supply.supplyTrace.alert.AlertResponse;
import com.supply.supplyTrace.snapshot.SnapshotComparisonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sites")
@RequiredArgsConstructor
@Validated
public class SiteController {

    private final MonitoredSiteService monitoredSiteService;

    @PostMapping
    public ResponseEntity<SiteResponse> createSite(@Valid @RequestBody CreateSiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(monitoredSiteService.createSite(request));
    }

    @GetMapping
    public List<SiteResponse> listSites() {
        return monitoredSiteService.listSites();
    }

    @GetMapping("/{id}")
    public SiteResponse getSite(@PathVariable Long id) {
        return monitoredSiteService.getSite(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(@PathVariable Long id) {
        monitoredSiteService.deleteSite(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/scan")
    public ScanResponse scanSite(@PathVariable Long id) {
        return monitoredSiteService.scanSite(id);
    }

    @GetMapping("/{id}/alerts")
    public List<AlertResponse> getSiteAlerts(@PathVariable Long id) {
        return monitoredSiteService.getAlerts(id);
    }

    @GetMapping("/{id}/snapshot/compare")
    public SnapshotComparisonResponse compareSnapshots(@PathVariable Long id) {
        return monitoredSiteService.getSnapshotComparison(id);
    }
}



