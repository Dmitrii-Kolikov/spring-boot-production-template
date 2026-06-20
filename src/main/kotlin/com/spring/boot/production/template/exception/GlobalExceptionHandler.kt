package com.spring.boot.production.template.exception

import com.spring.boot.production.template.model.BaseResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleAllUncaughtExceptions(ex: Exception): BaseResponse<Nothing> {
        return BaseResponse(false, null, ex.localizedMessage)
    }
}