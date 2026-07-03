package com.spring.boot.production.template.config.logging.fiegn

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LoggerConfig {

    @Bean
    fun customFeignClientLogger(
        objectMapper: ObjectMapper,
        @Value("\${feign.logging.enabled:false}") isFeignLoggingEnabled: Boolean,
        @Value("\${feign.masking.enabled:false}") isFeignMaskingEnabled: Boolean,
        @Value("\${feign.short.logs:false}") isFeignShortLogs: Boolean,
        @Value("\${feign.max.loggable.size:2048}") feignMaxLoggableSize: Int
    ): Logger {
        return CustomFeignClientLogger(objectMapper, isFeignLoggingEnabled, isFeignMaskingEnabled, isFeignShortLogs, feignMaxLoggableSize)
    }

    @Bean
    fun feignLoggerLevel(): Logger.Level {
        return Logger.Level.FULL
    }
}