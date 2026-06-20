package com.spring.boot.production.template.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseResponse<T>(
    val status: Boolean,
    val body: T? = null,
    val error: ErrorDto? = null
)