package com.spring.boot.production.template.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource
import java.nio.charset.StandardCharsets

class UtilsTest {
    companion object {
        fun jacksonObjectMapperMock(): ObjectMapper {
            return jacksonObjectMapper().apply {
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }

        inline fun <reified T> ObjectMapper.readJsonResource(path: String): T {
            return ClassPathResource(path).inputStream.use { stream ->
                this.readValue(stream)
            }
        }

        fun readTextResource(path: String): String {
            return ClassPathResource(path).inputStream.use { stream ->
                String(stream.readAllBytes(), StandardCharsets.UTF_8)
            }
        }
    }
}