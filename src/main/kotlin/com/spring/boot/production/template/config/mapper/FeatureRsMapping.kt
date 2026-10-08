package com.spring.boot.production.template.config.mapper

import com.spring.boot.production.template.api.dto.integration.FeatureIntegrationModelRqDto
import com.spring.boot.production.template.api.dto.integration.FeatureIntegrationModelRsDto
import com.spring.boot.production.template.api.dto.rest.ErrorMeetingDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelRsDto
import com.spring.boot.production.template.api.dto.rest.MeetingDto
import com.spring.boot.production.template.model.entity.FeatureEntity

fun FeatureIntegrationModelRsDto.toFeatureEntity(): FeatureEntity {
    val body = this
    return FeatureEntity(
        meetingId = body.id,
        description = body.description
    )
}

fun FeatureEntity.toFeatureModelRs(): FeatureModelRsDto {
    val body = this
    return FeatureModelRsDto().apply {
        id = body.id
        meetingId = body.meetingId
        description = body.description
    }
}

fun FeatureIntegrationModelRsDto.toMeetingRsFromEntity(): MeetingDto {
    val body = this
    return MeetingDto().apply {
        id = body.id
        description = body.description
    }
}

fun FeatureIntegrationModelRqDto.toErrorMeetingRs(error: String?): ErrorMeetingDto {
    val body = this
    return ErrorMeetingDto().apply {
        description = body.description
        errorDescription = error
    }
}

fun List<MeetingDto>.toFeaturesEntity(): List<FeatureEntity> {
    val body = this
    return body.map { it.toFeatureEntity() }
}

fun FeatureEntity.toMeetingRsFromEntity(): MeetingDto {
    val body = this
    return MeetingDto().apply {
        id = body.id
        meetingId = body.meetingId
        description = body.description
    }
}

private fun MeetingDto.toFeatureEntity(): FeatureEntity {
    val body = this
    return FeatureEntity(
        meetingId = body.id,
        description = body.description
    )
}