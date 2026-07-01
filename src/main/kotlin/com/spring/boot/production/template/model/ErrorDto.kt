package com.spring.boot.production.template.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDto(
    val code: String? = null,
    val title: String? = null,
    val subCode: String? = null,
    val description: String? = null,
    val rqUid: String? = null,
    val timestamp: String? = null,
)