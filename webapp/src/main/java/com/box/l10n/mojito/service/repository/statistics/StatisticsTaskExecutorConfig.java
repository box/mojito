package com.box.l10n.mojito.service.repository.statistics;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class StatisticsTaskExecutorConfig {

  @Value("${l10n.statistics.taskExecutor.corePoolSize:10}")
  int corePoolSize;

  @Value("${l10n.statistics.taskExecutor.maxPoolSize:50}")
  int maxPoolSize;

  @Value("${l10n.statistics.taskExecutor.queueCapacity:#{T(java.lang.Integer).MAX_VALUE}}")
  int queueCapacity;

  @Value("${l10n.statistics.taskExecutor.keepAliveSeconds:60}")
  int keepAliveSeconds;

  @Value("${l10n.statistics.taskExecutor.threadNamePrefix:statistics-task-executor}")
  String threadNamePrefix;

  @Value("${l10n.statistics.taskExecutor.rejectedPolicy:ABORT}")
  String rejectedPolicy;

  @Bean
  public TaskExecutor statisticsTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setKeepAliveSeconds(keepAliveSeconds);
    executor.setThreadNamePrefix(threadNamePrefix);
    setRejectionPolicy(executor);
    executor.initialize();
    return executor;
  }

  private void setRejectionPolicy(ThreadPoolTaskExecutor executor) {
    switch (rejectedPolicy.toUpperCase()) {
      case "ABORT":
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        break;
      case "CALLERRUN":
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        break;
      case "DISCARD":
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        break;
      case "DISCARDOLDEST":
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        break;
      default:
        throw new RuntimeException("Invalid rejectedPolicy: " + rejectedPolicy);
    }
  }
}
