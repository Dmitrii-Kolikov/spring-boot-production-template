package com.spring.boot.production.template.config.exception.feature

data class FeatureError(
    val code: String? = null,
    val message: String? = null,
)