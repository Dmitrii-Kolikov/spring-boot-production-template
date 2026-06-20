package com.spring.boot.production.template.service.impl

import com.spring.boot.production.template.model.FeatureModel
import com.spring.boot.production.template.service.api.IFeatureService
import com.spring.boot.production.template.util.withContextIO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service

@Service
class FeatureService: IFeatureService {

    override fun getFeature(): FeatureModel {
        return FeatureModel(1, "Важная встреча с клиентом")
    }

    override suspend fun getFeatureAsync(): FeatureModel {
        return coroutineScope {
            withContextIO {
                delay(2000)
            }
            FeatureModel(1, "Важная встреча с клиентом")
        }
    }
}