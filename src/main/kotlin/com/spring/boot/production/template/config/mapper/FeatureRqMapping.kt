package com.spring.boot.production.template.config.mapper

import com.spring.boot.production.template.api.dto.integration.FeatureIntegrationModelRqDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelListRqDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelRqDto

fun FeatureModelRqDto.toFeatureIntegrationModelRq(): FeatureIntegrationModelRqDto {
    val body = this
    return FeatureIntegrationModelRqDto().apply {
        description = body.description
    }
}

fun FeatureModelListRqDto.toFeatureIntegrationModelRq(): List<FeatureIntegrationModelRqDto> {
    val body = this
    return body.descriptions.map { it.description.toFeatureIntegrationModelRq() }
}

private fun String.toFeatureIntegrationModelRq(): FeatureIntegrationModelRqDto {
    val body = this
    return FeatureIntegrationModelRqDto().apply {
        description = body
    }
}