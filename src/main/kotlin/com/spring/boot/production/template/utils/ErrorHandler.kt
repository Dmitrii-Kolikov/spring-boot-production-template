package com.spring.boot.production.template.utils

import com.spring.boot.production.template.config.exception.BusinessException
import com.spring.boot.production.template.config.exception.ProductionException
import com.spring.boot.production.template.config.exception.TechnicalException
import com.spring.boot.production.template.config.header.Headers
import com.spring.boot.production.template.enums.ProductionError
import feign.Request
import org.slf4j.MDC

fun <T> handlerException(error: ProductionError, block: () -> T): T  {
    return runCatching { block() }
        .getOrElse {
            when (it) {
                is ProductionException -> throw BusinessException(
                    code = error.code,
                    title = error.title,
                    subCode = it.subCode,
                    description = it.description,
                    rqUid = it.request.toRqUid(),
                    timestamp = it.request.toTimestamp()
                )
                else -> throw TechnicalException(
                    code = error.code,
                    title = error.title,
                    subCode = ProductionError.TECHNICAL_ERROR.code,
                    description = "${ProductionError.TECHNICAL_ERROR.title} ${it.localizedMessage}",
                    rqUid = MDC.get(Headers.REQUEST_CHAIN_ID_HTTP_HEADER)
                )
            }
        }
}

fun Request.toRqUid(): String? = this.headers()[Headers.REQUEST_CHAIN_ID_HTTP_HEADER]?.firstOrNull()
fun Request.toTimestamp(): String? = this.headers()["rqtm"]?.firstOrNull()