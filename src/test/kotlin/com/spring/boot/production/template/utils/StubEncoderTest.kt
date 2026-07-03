package com.spring.boot.production.template.utils

import com.fasterxml.jackson.core.JsonProcessingException
import feign.RequestTemplate
import feign.Util
import feign.codec.EncodeException
import feign.codec.Encoder
import java.lang.reflect.Type

class StubEncoderTest: Encoder {
    private val objectMapper = UtilsTest.jacksonObjectMapperMock()

    override fun encode(body: Any, type: Type, template: RequestTemplate) {
        try {
            template.body(objectMapper.writerFor(objectMapper.constructType(type)).writeValueAsBytes(body), Util.UTF_8)
        } catch (e: JsonProcessingException) {
            throw EncodeException(e.message, e)
        }
    }
}