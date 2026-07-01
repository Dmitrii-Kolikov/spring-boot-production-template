package com.spring.boot.production.template.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.spring.boot.production.template.api.dto.rest.FeatureModelRqDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelRsDto
import com.spring.boot.production.template.service.api.IFeatureService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(FeatureController::class)
class FeatureControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvcTester

    @MockitoBean
    private lateinit var featureService: IFeatureService

    private val objectMapper = ObjectMapper()

    @Nested
    inner class GetFeatureControllerTest {
        @Test
        fun get_feature_controller_success_test() {
            whenever(featureService.getFeature(any())).thenReturn(
                FeatureModelRsDto().apply {
                    id = 1
                    description = "Важная встреча с клиентом"
                }
            )

            mockMvc.get().uri("/v1/features/feature/{meetingId}", 1)
                .assertThat()
                .hasStatusOk()
                .matches(jsonPath("$.status").value(true))
                .matches(jsonPath("$.body.id").value(1))
                .matches(jsonPath("$.body.description").value("Важная встреча с клиентом"))
        }

        @Test
        fun get_feature_controller_error_validation_test() {
            whenever(featureService.getFeature(any())).thenReturn(
                FeatureModelRsDto().apply {
                    id = 1
                }
            )

            mockMvc.get().uri("/v1/features/feature/{id}", 1)
                .assertThat()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .matches(jsonPath("$.status").value(false))
                .matches(jsonPath("$.error.code").value("IP-002"))
                .matches(jsonPath("$.error.title").value("Ошибка валидации запроса или ответа"))
                .matches(jsonPath("$.error.description").value("body.description must not be null"))
        }
    }

    @Nested
    inner class CreateFeatureControllerTest {
        @Test
        fun create_feature_controller_success_test() {
            val requestBody = FeatureModelRqDto().apply {
                description = "Важная встреча с клиентом"
            }

            whenever(featureService.createFeature(any())).thenReturn(
                FeatureModelRsDto().apply {
                    id = 1
                    description = "Важная встреча с клиентом"
                }
            )

            mockMvc.post()
                .uri("/v1/features/feature/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
                .assertThat()
                .matches(jsonPath("$.status").value(true))
                .matches(jsonPath("$.body.id").value(1))
                .matches(jsonPath("$.body.description").value("Важная встреча с клиентом"))
        }

        @Test
        fun create_feature_controller_error_validation_test() {
            val requestBody = FeatureModelRqDto().apply {
                description = null
            }

            whenever(featureService.createFeature(any())).thenReturn(
                FeatureModelRsDto().apply {
                    id = 1
                    description = "Важная встреча с клиентом"
                }
            )

            mockMvc.post()
                .uri("/v1/features/feature/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
                .assertThat()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .matches(jsonPath("$.status").value(false))
                .matches(jsonPath("$.error.code").value("IP-002"))
                .matches(jsonPath("$.error.title").value("Ошибка валидации запроса или ответа"))
                .matches(jsonPath("$.error.description").value("description must not be null"))
        }
    }
}