package com.spring.boot.production.template.model

data class BaseResponse<T>(
    val status: Boolean,
    val body: T? = null,
    val error: String? = null //feature model
)