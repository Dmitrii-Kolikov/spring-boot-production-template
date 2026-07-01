package com.spring.boot.production.template.exception

import com.spring.boot.production.template.dictonary.Headers
import com.spring.boot.production.template.enums.ProductionError
import com.spring.boot.production.template.model.BaseResponse
import com.spring.boot.production.template.model.ErrorDto
import com.spring.boot.production.template.util.Utils
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException

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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun methodArgumentNotValidException(ex: MethodArgumentNotValidException): BaseResponse<Nothing> {
        return BaseResponse(false, null, ErrorDto(
            code = ProductionError.VALIDATION_ERROR.code,
            title = ProductionError.VALIDATION_ERROR.title,
            description = ex.bindingResult.fieldErrors.joinToString { "${it.field} ${it.defaultMessage}" },
            rqUid = MDC.get(Headers.REQUEST_CHAIN_ID_HTTP_HEADER),
            timestamp = Utils.rqTm()
        ))
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handlerMethodValidationException(ex: HandlerMethodValidationException): BaseResponse<Nothing> {
        return BaseResponse(false, null, ErrorDto(
            code = ProductionError.VALIDATION_ERROR.code,
            title = ProductionError.VALIDATION_ERROR.title,
            description = ex.beanResults.joinToString { "${it.fieldError?.field} ${it.fieldError?.defaultMessage}" },
            rqUid = MDC.get(Headers.REQUEST_CHAIN_ID_HTTP_HEADER),
            timestamp = Utils.rqTm()
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