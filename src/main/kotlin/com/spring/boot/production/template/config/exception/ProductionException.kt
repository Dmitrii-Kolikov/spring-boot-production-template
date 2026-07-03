package com.spring.boot.production.template.config.exception

import feign.Request

data class ProductionException(
    val subCode: String? = null,
    val description: String? = null,
    val request: Request,
) : RuntimeException()