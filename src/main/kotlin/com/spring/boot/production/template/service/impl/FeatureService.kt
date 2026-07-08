package com.spring.boot.production.template.service.impl

import com.spring.boot.production.template.api.dto.rest.FeatureModelRqDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelRsDto
import com.spring.boot.production.template.model.entity.FeatureEntity
import com.spring.boot.production.template.repository.jpa.ProductionRepository
import com.spring.boot.production.template.service.api.IFeatureService
import com.spring.boot.production.template.utils.withContextIO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class FeatureService(
    private val productionRepository: ProductionRepository
): IFeatureService {

    override fun getFeature(meetingId: Long): FeatureModelRsDto {
        val rs = productionRepository.findByIdOrNull(meetingId)
        return FeatureModelRsDto().apply {
            id = rs?.id
            description = rs?.description
        }
    }

    override suspend fun getFeatureAsync(): FeatureModelRsDto {
        return coroutineScope {
            withContextIO {
                delay(2000)
            }
            FeatureModelRsDto().apply {
                id = 1
                description = "Важная встреча с клиентом"
            }
        }
    }

    override fun createFeature(rq: FeatureModelRqDto): FeatureModelRsDto {
        val entityRq = FeatureEntity(description = rq.description)
        val rs = productionRepository.saveAndFlush(entityRq)
        return FeatureModelRsDto().apply {
            id = rs.id
            description = rs.description
        }
    }
}