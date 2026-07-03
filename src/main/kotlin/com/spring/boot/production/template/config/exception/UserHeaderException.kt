package com.spring.boot.production.template.config.exception

data class UserHeaderException(
    val code: String,
    val description: String
): RuntimeException()