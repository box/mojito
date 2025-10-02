package com.box.l10n.mojito.rest.monitoring;

import com.box.l10n.mojito.service.monitoring.DbMonitoringService;
import com.box.l10n.mojito.service.monitoring.DbMonitoringService.DbMonitoringSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring")
public class DbMonitoringWS {

  static final int DEFAULT_ITERATIONS = 5;

  private final DbMonitoringService dbMonitoringService;

  public DbMonitoringWS(DbMonitoringService dbMonitoringService) {
    this.dbMonitoringService = dbMonitoringService;
  }

  @GetMapping("/db")
  public DbMonitoringSnapshot getDatabaseLatency(
      @RequestParam(name = "iterations", defaultValue = "" + DEFAULT_ITERATIONS) int iterations) {

    int sanitizedIterations =
        Math.max(
            DbMonitoringService.MIN_ITERATIONS,
            Math.min(DbMonitoringService.MAX_ITERATIONS, iterations));

    return dbMonitoringService.measureLatency(sanitizedIterations);
  }
}
