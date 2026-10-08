package com.spring.boot.production.template.service.impl

import com.spring.boot.production.template.api.dto.rest.ErrorMeetingDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelListRqDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelListRsDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelRqDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelRsDto
import com.spring.boot.production.template.api.dto.rest.MeetingDto
import com.spring.boot.production.template.config.exception.BusinessException
import com.spring.boot.production.template.config.exception.TechnicalException
import com.spring.boot.production.template.config.mapper.toErrorMeetingRs
import com.spring.boot.production.template.config.mapper.toFeatureEntity
import com.spring.boot.production.template.config.mapper.toFeatureIntegrationModelRq
import com.spring.boot.production.template.config.mapper.toFeatureModelRs
import com.spring.boot.production.template.config.mapper.toFeaturesEntity
import com.spring.boot.production.template.config.mapper.toMeetingRsFromEntity
import com.spring.boot.production.template.enums.ProductionError
import com.spring.boot.production.template.repository.feign.ProductionClient
import com.spring.boot.production.template.service.api.IFeatureService
import com.spring.boot.production.template.service.data.ProductionDataRepository
import com.spring.boot.production.template.utils.handlerException
import com.spring.boot.production.template.utils.mapAsyncDeferred
import com.spring.boot.production.template.utils.withContextIO
import kotlinx.coroutines.supervisorScope
import org.springframework.stereotype.Service

@Service
class FeatureService(
    private val productionDataRepository: ProductionDataRepository,
    private val productionClient: ProductionClient
): IFeatureService {

    override fun getFeature(meetingId: Long): FeatureModelRsDto {
        return productionDataRepository.getFeatureByIdEntity(meetingId).toFeatureModelRs()
    }

    override suspend fun createFeatureAsync(rq: FeatureModelListRqDto): FeatureModelListRsDto {
        return supervisorScope {
            val successMeetings = mutableListOf<MeetingDto>()
            val errorMeetings = mutableListOf<ErrorMeetingDto>()

            val featureIntegrationMapperRq = rq.toFeatureIntegrationModelRq()
            val features = featureIntegrationMapperRq.mapAsyncDeferred(scope = this) { integration ->
                handlerException(ProductionError.ERR_CREATE_PRODUCTION) {
                    productionClient.createMeeting(integration)
                }
            }

            features.mapIndexed { index, deferred ->
                val featureIntegrationModelRq = featureIntegrationMapperRq[index]
                try {
                    val rs = deferred.await()
                    successMeetings.add(rs.toMeetingRsFromEntity())
                } catch (e: BusinessException) {
                    errorMeetings.add(featureIntegrationModelRq.toErrorMeetingRs(e.description))
                } catch (e: TechnicalException) {
                    errorMeetings.add(featureIntegrationModelRq.toErrorMeetingRs(e.description))
                }
            }

            val savedMeetings  = successMeetings.takeIf { it.isNotEmpty() }?.let {
                withContextIO {
                    productionDataRepository.saveAllFeaturesEntity(successMeetings.toFeaturesEntity())
                        .map { it.toMeetingRsFromEntity() }
                }
            } ?: emptyList()

            FeatureModelListRsDto().apply {
                this.meetings = savedMeetings
                this.errorMeetings = errorMeetings
            }
        }
    }

    override fun createFeature(rq: FeatureModelRqDto): FeatureModelRsDto {
        val product = handlerException(ProductionError.ERR_CREATE_PRODUCTION) {
            productionClient.createMeeting(rq.toFeatureIntegrationModelRq())
        }
        return productionDataRepository.saveFeatureEntity(product.toFeatureEntity()).toFeatureModelRs()
    }
}