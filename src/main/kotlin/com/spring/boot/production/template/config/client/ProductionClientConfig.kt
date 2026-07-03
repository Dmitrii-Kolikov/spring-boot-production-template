package com.spring.boot.production.template.config.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.spring.boot.production.template.config.exception.feature.ProductionErrorDecoder
import com.spring.boot.production.template.config.header.Headers
import com.spring.boot.production.template.utils.Utils
import feign.RequestInterceptor
import feign.RequestTemplate
import feign.codec.ErrorDecoder
import org.slf4j.MDC
import org.springframework.context.annotation.Bean

class ProductionClientConfig {

    @Bean
    fun errorDecoder(objectMapper: ObjectMapper): ErrorDecoder {
        return ProductionErrorDecoder(objectMapper)
    }

    @Bean
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { requestTemplate: RequestTemplate ->
            requestTemplate.header(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, MDC.get(Headers.REQUEST_CHAIN_ID_HTTP_HEADER))
            requestTemplate.header(Headers.SOURCE_SYSTEM, MDC.get(Headers.SOURCE_SYSTEM))
            requestTemplate.header("rqtm", Utils.rqTm())
        }
    }
}