package com.supply.supplyTrace.site;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoredSiteRepository extends JpaRepository<MonitoredSite, Long> {

    Optional<MonitoredSite> findByUrl(String url);
}

