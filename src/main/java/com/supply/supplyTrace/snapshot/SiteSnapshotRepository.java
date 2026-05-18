package com.supply.supplyTrace.snapshot;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.supply.supplyTrace.site.MonitoredSite;

public interface SiteSnapshotRepository extends JpaRepository<SiteSnapshot, Long> {

    Optional<SiteSnapshot> findTopBySiteOrderByScannedAtDesc(MonitoredSite site);

    Optional<SiteSnapshot> findTopBySiteOrderByScannedAtAsc(MonitoredSite site);

    List<SiteSnapshot> findBySiteOrderByScannedAtDesc(MonitoredSite site);

    void deleteBySite(MonitoredSite site);

    long countBySite(MonitoredSite site);
}

