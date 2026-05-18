package com.supply.supplyTrace.alert;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.supply.supplyTrace.site.MonitoredSite;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findBySiteOrderByCreatedAtDesc(MonitoredSite site);

    List<Alert> findTop10ByOrderByCreatedAtDesc();

    long countBySeverity(AlertSeverity severity);

    void deleteBySite(MonitoredSite site);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.site = :site AND a.type = :type AND a.message = :message AND a.createdAt > :since")
    long countRecentAlert(@Param("site") MonitoredSite site, @Param("type") AlertType type, @Param("message") String message, @Param("since") Instant since);

    long countBySite(MonitoredSite site);

    long countBySiteAndSeverity(MonitoredSite site, AlertSeverity severity);
}

