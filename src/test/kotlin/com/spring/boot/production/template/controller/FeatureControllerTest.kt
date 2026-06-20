package com.spring.boot.production.template.controller

import com.spring.boot.production.template.model.FeatureModelRs
import com.spring.boot.production.template.service.api.IFeatureService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(FeatureController::class)
class FeatureControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvcTester

    @MockitoBean
    private lateinit var featureService: IFeatureService

    @Test
    fun get_feature_controller_success_test() {
        whenever(featureService.getFeature(any())).thenReturn(FeatureModelRs(1, "Важная встреча с клиентом"))

        mockMvc.get().uri("/v1/features/feature/{id}", 1)
            .assertThat()
            .hasStatusOk()
            .matches(jsonPath("$.status").value(true))
            .matches(jsonPath("$.body.id").value(1))
            .matches(jsonPath("$.body.description").value("Важная встреча с клиентом"))
    }
}