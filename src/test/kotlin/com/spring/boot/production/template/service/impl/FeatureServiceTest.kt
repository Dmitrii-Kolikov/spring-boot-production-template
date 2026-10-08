package com.spring.boot.production.template.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.spring.boot.production.template.api.dto.integration.FeatureIntegrationModelRsDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelListRqDto
import com.spring.boot.production.template.config.exception.BusinessException
import com.spring.boot.production.template.config.exception.ProductionException
import com.spring.boot.production.template.enums.ProductionError
import com.spring.boot.production.template.model.entity.FeatureEntity
import com.spring.boot.production.template.repository.feign.ProductionClient
import com.spring.boot.production.template.service.data.ProductionDataRepository
import com.spring.boot.production.template.utils.UtilsTest.Companion.jacksonObjectMapperMock
import com.spring.boot.production.template.utils.UtilsTest.Companion.readJsonResource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

class FeatureServiceTest {
    private val productionDataRepository: ProductionDataRepository = mock()
    private val productionClient: ProductionClient = mock()
    private val featureService = FeatureService(productionDataRepository, productionClient)
    private val objectMapper: ObjectMapper = jacksonObjectMapperMock()

    @Test
    fun get_feature_service_success_test() {
        whenever(productionDataRepository.getFeatureByIdEntity(any())).thenReturn(
            FeatureEntity(1L, 10000000000166982L, "Важная встреча с клиентом")
        )

        val result = featureService.getFeature(1)

        verify(productionDataRepository, times(1)).getFeatureByIdEntity(any())

        assertEquals(1, result.id)
        assertEquals(10000000000166982, result.meetingId)
        assertEquals("Важная встреча с клиентом", result.description)
    }

    @Test
    fun get_feature_service_error_test() {
        whenever(productionDataRepository.getFeatureByIdEntity(any())).thenThrow(
            BusinessException(
                code = ProductionError.DATABASE_ERROR.code,
                title = ProductionError.DATABASE_ERROR.title,
                description = "Запись 1 не найдена"
            )
        )

        val result = assertFailsWith<BusinessException> {
            featureService.getFeature(1)
        }

        verify(productionDataRepository, times(1)).getFeatureByIdEntity(any())

        assertEquals(ProductionError.DATABASE_ERROR.code, result.code)
        assertEquals(ProductionError.DATABASE_ERROR.title, result.title)
        assertEquals("Запись 1 не найдена", result.description)
    }

    @Test
    fun create_feature_async_should_handle_success_and_errors_concurrently_service_success_test() {
        runTest {
            val request: FeatureModelListRqDto = objectMapper.readJsonResource("json/FeatureModelListRq.json")
            val response: FeatureIntegrationModelRsDto = objectMapper.readJsonResource("json/FeatureIntegrationModelRs.json")
            val responseEntity: List<FeatureEntity> = objectMapper.readJsonResource("json/SaveAllLFeaturesEntityRs.json")

            val businessException = ProductionException(
                subCode = "010",
                description = "Не удалось создать встречу из-за непредвиденной ошибки",
                request = mock<feign.Request>()
            )

            whenever(productionClient.createMeeting(argThat { this.description == "Успешная встреча" }))
                .thenReturn(response)

            whenever(productionClient.createMeeting(argThat { this.description == "Встреча с бизнес ошибкой" }))
                .thenThrow(businessException)

            whenever(productionDataRepository.saveAllFeaturesEntity(any())).thenReturn(responseEntity)

            val result = featureService.createFeatureAsync(request)

            verify(productionClient, times(2)).createMeeting(any())
            verify(productionDataRepository, times(1)).saveAllFeaturesEntity(any())

            // Проверяем успешные встречи
            assertEquals(1, result.meetings.size)
            assertEquals(1L, result.meetings[0].id)
            assertEquals(10000000000166982L, result.meetings[0].meetingId)
            assertEquals("Успешная встреча", result.meetings[0].description)

            // Проверяем упавшие встречи (errorMeetings)
            assertEquals(1, result.errorMeetings.size)
            assertEquals("Встреча с бизнес ошибкой", result.errorMeetings[0].description)
            assertEquals("Не удалось создать встречу из-за непредвиденной ошибки", result.errorMeetings[0].errorDescription)
        }
    }
}