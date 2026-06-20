package com.spring.boot.production.template.service.impl

import com.spring.boot.production.template.model.FeatureModelRq
import com.spring.boot.production.template.model.FeatureModelRs
import com.spring.boot.production.template.model.entity.FeatureEntity
import com.spring.boot.production.template.repository.FeatureRepository
import com.spring.boot.production.template.service.api.IFeatureService
import com.spring.boot.production.template.util.withContextIO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class FeatureService(
    private val featureRepository: FeatureRepository
): IFeatureService {

    override fun getFeature(id: Long): FeatureModelRs {
        val rs = featureRepository.findByIdOrNull(id)
        return FeatureModelRs(rs?.id, rs?.description)
    }

    override suspend fun getFeatureAsync(): FeatureModelRs {
        return coroutineScope {
            withContextIO {
                delay(2000)
            }
            FeatureModelRs(1, "Важная встреча с клиентом")
        }
    }

    override fun createFeature(rq: FeatureModelRq): FeatureModelRs {
        val entityRq = FeatureEntity(description = rq.description)
        val rs = featureRepository.saveAndFlush(entityRq)
        return FeatureModelRs(rs.id, rs.description)
    }
}