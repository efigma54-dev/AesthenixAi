package com.aicode.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async + scheduling configuration.
 *
 * @EnableScheduling activates @Scheduled methods (e.g. ReviewJobService.cleanupExpiredJobs).
 * @EnableAsync activates @Async methods.
 *
 * Worker pool sizing is CPU-aware:
 *   - core-size = 0 → auto: Runtime.getRuntime().availableProcessors()
 *   - max-size  = 0 → auto: availableProcessors * 2
 * Both can be overridden via application.yml / env vars for production tuning.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${worker.core-size:0}")
    private int configuredCoreSize;

    @Value("${worker.max-size:0}")
    private int configuredMaxSize;

    /**
     * Dedicated executor for PR bot analysis tasks.
     * Isolated from the common pool — webhook bursts won't starve other async work.
     *
     * Core and max sizes are CPU-aware when config values are 0 (default).
     */
    @Bean(name = "prBotExecutor")
    public Executor prBotExecutor() {
        int cpus     = Runtime.getRuntime().availableProcessors();
        int coreSize = configuredCoreSize > 0 ? configuredCoreSize : cpus;
        int maxSize  = configuredMaxSize  > 0 ? configuredMaxSize  : cpus * 2;

        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(coreSize);
        ex.setMaxPoolSize(maxSize);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("pr-bot-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(60);
        ex.initialize();

        log.info("Worker pool configured — coreSize={}, maxSize={}, availableProcessors={}",
                coreSize, maxSize, cpus);

        return ex;
    }
}
