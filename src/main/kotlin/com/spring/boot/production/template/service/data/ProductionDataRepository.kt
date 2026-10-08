package com.spring.boot.production.template.service.data

import com.spring.boot.production.template.config.exception.BusinessException
import com.spring.boot.production.template.config.header.Headers
import com.spring.boot.production.template.enums.ProductionError
import com.spring.boot.production.template.model.entity.FeatureEntity
import com.spring.boot.production.template.repository.jpa.ProductionRepository
import com.spring.boot.production.template.utils.Utils.rqTm
import org.slf4j.MDC
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductionDataRepository(
    private val productionRepository: ProductionRepository
) {

    @Transactional
    fun saveFeatureEntity(featureEntity: FeatureEntity) : FeatureEntity {
        return productionRepository.saveAndFlush(featureEntity)
    }

    @Transactional
    fun saveAllFeaturesEntity(featuresEntity: List<FeatureEntity>) : List<FeatureEntity> {
        return productionRepository.saveAllAndFlush(featuresEntity)
    }


    @Transactional(readOnly = true)
    fun getFeatureByIdEntity(meetingId: Long) : FeatureEntity {
        return productionRepository.findByIdOrNull(meetingId)
            ?: throw BusinessException(
                code = ProductionError.DATABASE_ERROR.code,
                title = ProductionError.DATABASE_ERROR.title,
                description = "Запись $meetingId не найдена",
                rqUid = MDC.get(Headers.REQUEST_CHAIN_ID_HTTP_HEADER),
                timestamp = rqTm()
            )
    }
}