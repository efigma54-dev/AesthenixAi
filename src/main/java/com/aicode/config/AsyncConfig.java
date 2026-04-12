package com.aicode.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's @Async support.
 * Thread pool is configured via spring.task.execution in application.yml.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
