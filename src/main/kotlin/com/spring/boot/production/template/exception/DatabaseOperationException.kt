package com.spring.boot.production.template.exception

class DatabaseOperationException(
    val code: String? = null,
    val title: String? = null,
    val subCode: String? = null,
    val description: String? = null,
    val rqUid: String? = null,
    val timestamp: String? = null,
) : RuntimeException()