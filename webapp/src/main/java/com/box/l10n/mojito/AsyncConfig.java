package com.box.l10n.mojito;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

/**
 * @author jaurambault
 */
@Configuration
@EnableAsync(mode = AdviceMode.ASPECTJ)
public class AsyncConfig extends AsyncConfigurerSupport {

  /**
   * When using aspectJ + @EnableAsync, there is no default executor. Need to create one.
   *
   * @return
   */
  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setBeanName("asyncExecutor");
    threadPoolTaskExecutor.setCorePoolSize(5);
    threadPoolTaskExecutor.setMaxPoolSize(10);
    threadPoolTaskExecutor.initialize();
    return threadPoolTaskExecutor;
  }

  @Bean(name = "pollableTaskExecutor")
  public AsyncTaskExecutor getPollableTaskExecutor() {
    ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setBeanName("pollableTask");
    threadPoolTaskExecutor.setCorePoolSize(5);
    threadPoolTaskExecutor.setMaxPoolSize(30);
    threadPoolTaskExecutor.initialize();

    return new DelegatingSecurityContextAsyncTaskExecutor(threadPoolTaskExecutor);
  }
}
