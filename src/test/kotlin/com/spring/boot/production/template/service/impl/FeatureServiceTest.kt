package com.spring.boot.production.template.service.impl

import com.spring.boot.production.template.model.entity.FeatureEntity
import com.spring.boot.production.template.repository.jpa.ProductionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

class FeatureServiceTest {
    private val productionRepository: ProductionRepository = mock()
    private val featureService = FeatureService(productionRepository)

    @Test
    fun get_feature_service_success_test() {
        whenever(productionRepository.findById(any())).thenReturn(Optional.of(FeatureEntity(1, "Важная встреча с клиентом")))
        val result = featureService.getFeature(1)

        assertEquals(1, result.id)
        assertEquals("Важная встреча с клиентом", result.description)
    }
}