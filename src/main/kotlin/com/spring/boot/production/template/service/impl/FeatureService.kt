package com.spring.boot.production.template.service.impl

import com.spring.boot.production.template.service.api.IFeatureService
import org.springframework.stereotype.Service

@Service
class FeatureService: IFeatureService {

    override fun getFeature(): String {
        return "Feature api"
    }
}