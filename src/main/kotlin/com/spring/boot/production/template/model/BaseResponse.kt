package com.spring.boot.production.template.model

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseResponse<T>(
    val status: Boolean,
    @field:Valid
    val body: T? = null,
    val error: ErrorDto? = null
)