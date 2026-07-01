package com.spring.boot.production.template.exception

import com.spring.boot.production.template.dictonary.Headers
import com.spring.boot.production.template.enums.ProductionError
import com.spring.boot.production.template.model.BaseResponse
import com.spring.boot.production.template.model.ErrorDto
import com.spring.boot.production.template.util.Utils
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DatabaseOperationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun databaseOperationException(ex: DatabaseOperationException): BaseResponse<Nothing> {
        return BaseResponse(false, null, ErrorDto(
            code = ex.code,
            title = ex.title,
            subCode = ex.subCode,
            description = ex.description,
            rqUid = ex.rqUid,
            timestamp = ex.timestamp
        ))
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleAllUncaughtExceptions(ex: Exception): BaseResponse<Nothing> {
        return BaseResponse(false, null,  ErrorDto(
            code = ProductionError.TECHNICAL_ERROR.code,
            title = ProductionError.TECHNICAL_ERROR.title,
            description = ex.localizedMessage,
            rqUid = MDC.get(Headers.REQUEST_CHAIN_ID_HTTP_HEADER),
            timestamp = Utils.rqTm()
        ))
    }
}