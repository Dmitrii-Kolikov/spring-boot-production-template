package com.spring.boot.production.template.logging.mask

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.MapFunction
import com.jayway.jsonpath.Option
import org.slf4j.LoggerFactory

object JsonStreamMasker {
    private val log = LoggerFactory.getLogger(JsonStreamMasker::class.java)
    private val jsonPathConfig = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS)

    fun mask(body: String?, maskFields: Array<MaskField>?): String? {
        if (body.isNullOrBlank() || maskFields.isNullOrEmpty()) {
            return body
        }

        return try {
            val documentContext = JsonPath.using(jsonPathConfig).parse(body)
            for (path in maskFields) {
                documentContext.map(path.name, MapFunction { currentValue, _ ->
                    currentValue?.toString()?.let {
                        path.type.mask(it)
                    }
                })
            }
            documentContext.jsonString()
        } catch (ex: Exception) {
            log.error("Ошибка маскирования JSON лога: ${ex.message}", ex)
            body
        }
    }
}