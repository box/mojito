package com.box.l10n.mojito.service.monitoring;

import com.box.l10n.mojito.service.repository.RepositoryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

/** Provides utilities to measure database latency by executing lightweight probes. */
@Service
public class DbMonitoringService {

  public static final int MIN_ITERATIONS = 1;
  public static final int MAX_ITERATIONS = 20;

  private static final String HEALTH_QUERY = "SELECT 1";

  private final RepositoryService repositoryService;
  private final DataSource dataSource;

  @PersistenceContext EntityManager entityManager;

  public DbMonitoringService(RepositoryService repositoryService, DataSource dataSource) {
    this.repositoryService = repositoryService;
    this.dataSource = dataSource;
  }

  public DbMonitoringSnapshot measureLatency(int requestedIterations) {
    int iterations = Math.max(MIN_ITERATIONS, Math.min(MAX_ITERATIONS, requestedIterations));

    LatencySeries rawJdbcSeries = measureSeries(iterations, this::probeRawJdbcHealth);
    LatencySeries hibernateHealthSeries = measureSeries(iterations, this::probeHibernateHealth);
    LatencySeries hibernateRepoSeries = measureSeries(iterations, this::probeHibernateRepositories);

    return new DbMonitoringSnapshot(
        Instant.now(), iterations, rawJdbcSeries, hibernateHealthSeries, hibernateRepoSeries);
  }

  private LatencySeries measureSeries(int iterations, Runnable probe) {
    List<LatencyMeasurement> measurements = new ArrayList<>(iterations);

    for (int i = 0; i < iterations; i++) {
      long start = System.nanoTime();
      probe.run();
      long elapsedNanos = System.nanoTime() - start;
      double elapsedMillis = elapsedNanos / (double) TimeUnit.MILLISECONDS.toNanos(1);
      measurements.add(new LatencyMeasurement(i + 1, elapsedMillis));
    }

    DoubleSummaryStatistics stats =
        measurements.stream().mapToDouble(LatencyMeasurement::getLatencyMs).summaryStatistics();

    return new LatencySeries(measurements, stats.getMin(), stats.getMax(), stats.getAverage());
  }

  private void probeRawJdbcHealth() {
    executeRaw(HEALTH_QUERY);
  }

  private void probeHibernateHealth() {
    entityManager.createNativeQuery(HEALTH_QUERY).getSingleResult();
    entityManager.clear();
  }

  private void probeHibernateRepositories() {
    repositoryService.findRepositoriesIsNotDeletedOrderByName(null);
    entityManager.clear();
  }

  private void executeRaw(String sql) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.execute();
    } catch (SQLException e) {
      throw new RuntimeException("Unable to execute raw JDBC probe", e);
    }
  }

  public static class DbMonitoringSnapshot {
    final Instant timestamp;
    final int iterations;
    final LatencySeries raw;
    final LatencySeries hibernateHealth;
    final LatencySeries hibernateRepo;

    public DbMonitoringSnapshot(
        Instant timestamp,
        int iterations,
        LatencySeries raw,
        LatencySeries hibernateHealth,
        LatencySeries hibernateRepo) {
      this.timestamp = timestamp;
      this.iterations = iterations;
      this.raw = raw;
      this.hibernateHealth = hibernateHealth;
      this.hibernateRepo = hibernateRepo;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public int getIterations() {
      return iterations;
    }

    public LatencySeries getRaw() {
      return raw;
    }

    public LatencySeries getHibernateHealth() {
      return hibernateHealth;
    }

    public LatencySeries getHibernateRepo() {
      return hibernateRepo;
    }
  }

  public static class LatencySeries {
    final List<LatencyMeasurement> measurements;
    final double minLatencyMs;
    final double maxLatencyMs;
    final double averageLatencyMs;

    public LatencySeries(
        List<LatencyMeasurement> measurements,
        double minLatencyMs,
        double maxLatencyMs,
        double averageLatencyMs) {
      this.measurements = List.copyOf(measurements);
      this.minLatencyMs = minLatencyMs;
      this.maxLatencyMs = maxLatencyMs;
      this.averageLatencyMs = averageLatencyMs;
    }

    public List<LatencyMeasurement> getMeasurements() {
      return measurements;
    }

    public double getMinLatencyMs() {
      return minLatencyMs;
    }

    public double getMaxLatencyMs() {
      return maxLatencyMs;
    }

    public double getAverageLatencyMs() {
      return averageLatencyMs;
    }
  }

  public static class LatencyMeasurement {
    final int iteration;
    final double latencyMs;

    LatencyMeasurement(int iteration, double latencyMs) {
      this.iteration = iteration;
      this.latencyMs = latencyMs;
    }

    public int getIteration() {
      return iteration;
    }

    public double getLatencyMs() {
      return latencyMs;
    }
  }
}
