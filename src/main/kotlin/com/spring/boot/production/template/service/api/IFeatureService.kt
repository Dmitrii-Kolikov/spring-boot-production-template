package com.spring.boot.production.template.service.api

import com.spring.boot.production.template.api.dto.rest.FeatureModelRqDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelRsDto

interface IFeatureService {
    fun getFeature(meetingId: Long): FeatureModelRsDto
    suspend fun getFeatureAsync(): FeatureModelRsDto
    fun createFeature(rq: FeatureModelRqDto): FeatureModelRsDto
}