package com.spring.boot.production.template.utils

import feign.Response
import feign.codec.Decoder
import java.lang.reflect.Type

class StubDecoderTest: Decoder {
    private val objectMapper = UtilsTest.jacksonObjectMapperMock()

    override fun decode(response: Response, type: Type): Any? {
        return objectMapper.readValue(response.body().asInputStream().use { it.readAllBytes() }, objectMapper.constructType(type))
    }
}