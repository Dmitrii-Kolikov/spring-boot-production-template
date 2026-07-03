package com.spring.boot.production.template.config.exception.feature

import com.fasterxml.jackson.databind.ObjectMapper
import com.spring.boot.production.template.config.exception.ProductionException
import feign.Response
import feign.codec.ErrorDecoder
import java.lang.Exception

class ProductionErrorDecoder(
    private val objectMapper: ObjectMapper
) : ErrorDecoder {

    override fun decode(methodKey: String, response: Response): Exception {
        val bodyBytes = response.body()?.asInputStream()?.use { it.readAllBytes() }
        val body = runCatching { objectMapper.readValue(bodyBytes, FeatureError::class.java) }.getOrNull()
        return ProductionException(
            description =  body?.let { "${it.code} ${it.message}" } ?: "С переданными параметрами возникла ошибка в FEATURE",
            request = response.request()
        )
    }

}