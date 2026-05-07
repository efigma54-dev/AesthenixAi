package com.aicode.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Loads environment variables from .env file.
 * 
 * This allows Spring @Value annotations to read from .env
 * without requiring manual environment variable export.
 */
@Configuration
@PropertySource(value = "file:.env", ignoreResourceNotFound = true)
public class EnvConfig {
    // Configuration class — no beans needed
}
