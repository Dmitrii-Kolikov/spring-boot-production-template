package com.spring.boot.production.template.service.api

import com.spring.boot.production.template.model.FeatureModel

interface IFeatureService {
    fun getFeature(): FeatureModel
    suspend fun getFeatureAsync(): FeatureModel
}