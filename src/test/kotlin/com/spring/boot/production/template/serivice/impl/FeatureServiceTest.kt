package com.spring.boot.production.template.serivice.impl

import com.spring.boot.production.template.service.impl.FeatureService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeatureServiceTest {

    private val featureService = FeatureService()

    @Test
    fun get_feature_service_success_test() {
        val result = featureService.getFeature()

        assertEquals(1, result.id)
        assertEquals("Важная встреча с клиентом", result.description)
    }
}