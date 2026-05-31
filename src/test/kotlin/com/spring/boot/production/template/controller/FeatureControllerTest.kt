package com.spring.boot.production.template.controller

import com.spring.boot.production.template.service.api.IFeatureService

import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester

@WebMvcTest(FeatureController::class)
class FeatureControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvcTester

    @MockitoBean
    private lateinit var featureService: IFeatureService

    @Test
    fun get_feature_controller_success_test() {
        given(featureService.getFeature()).willReturn("feature api")

        mockMvc.get().uri("/v1/features/feature")
            .assertThat()
            .hasStatusOk()
            .hasBodyTextEqualTo("feature api")
    }
}