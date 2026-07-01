package com.spring.boot.production.template.service.api

import com.spring.boot.production.template.model.FeatureModelRq
import com.spring.boot.production.template.model.FeatureModelRs

interface IFeatureService {
    fun getFeature(id: Long): FeatureModelRs
    suspend fun getFeatureAsync(): FeatureModelRs
    fun createFeature(rq: FeatureModelRq): FeatureModelRs
}