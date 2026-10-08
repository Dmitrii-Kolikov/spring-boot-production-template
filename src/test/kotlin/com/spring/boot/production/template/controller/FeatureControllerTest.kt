package com.spring.boot.production.template.controller

import com.spring.boot.production.template.api.dto.rest.FeatureModelListRsDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelRsDto
import com.spring.boot.production.template.config.header.Headers
import com.spring.boot.production.template.service.api.IFeatureService
import com.spring.boot.production.template.utils.UtilsTest
import com.spring.boot.production.template.utils.UtilsTest.Companion.readJsonResource
import com.spring.boot.production.template.utils.UtilsTest.Companion.readTextResource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import java.util.UUID

@WebMvcTest(FeatureController::class)
class FeatureControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var featureService: IFeatureService

    private val objectMapper = UtilsTest.jacksonObjectMapperMock()

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

            mockMvc.get("/v1/features/feature/{meetingId}", 1) {
                header(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, UUID.randomUUID().toString())
                header(Headers.SOURCE_SYSTEM, "example")
            }.andExpect {
                status { isOk() }

                jsonPath("$.status") { value(true) }
                jsonPath("$.body.id") { value(1) }
                jsonPath("$.body.description") { value("Важная встреча с клиентом") }
            }
        }

        @Test
        fun get_feature_controller_error_validation_test() {
            whenever(featureService.getFeature(any())).thenReturn(
                FeatureModelRsDto().apply {
                    id = 1
                }
            )

            mockMvc.get("/v1/features/feature/{id}", 1) {
                header(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, UUID.randomUUID().toString())
                header(Headers.SOURCE_SYSTEM, "example")
            }.andExpect {
                status { isBadRequest() }

                jsonPath("$.status") { value(false) }
                jsonPath("$.error.code") { value("IP-002") }
                jsonPath("$.error.title") { value("Ошибка валидации запроса или ответа") }
                jsonPath("$.error.description") { value("body.description must not be null") }
            }
        }
    }

    @Nested
    inner class CreateFeatureAsyncControllerTest {

        @Test
        fun create_feature_async_controller_success_test() {
            runTest {
                val request = readTextResource("json/FeatureModelListRq.json")
                val response: FeatureModelListRsDto = objectMapper.readJsonResource("json/FeatureModelListRs.json")

                whenever(featureService.createFeatureAsync(any())).thenReturn(response)

                val mmvResult = mockMvc.post("/v1/features/feature/async") {
                    header(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, UUID.randomUUID().toString())
                    header(Headers.SOURCE_SYSTEM, "example")
                    contentType = MediaType.APPLICATION_JSON
                    content = request
                }.andExpect {
                    request { asyncStarted() }
                }.andReturn()

                ResultActionsDsl(mockMvc.perform(asyncDispatch(mmvResult)), mockMvc)
                    .andExpect {
                        status { isOk() }
                        jsonPath("$.status") { value(true) }

                        jsonPath("$.body.meetings.size()") { value(1) }
                        jsonPath("$.body.meetings[0].id") { value(1) }
                        jsonPath("$.body.meetings[0].meetingId") { value(10000000000166982L) }
                        jsonPath("$.body.meetings[0].description") { value("Успешная встреча") }

                        jsonPath("$.body.errorMeetings.size()") { value(1) }
                        jsonPath("$.body.errorMeetings[0].description") { value("Встреча с бизнес ошибкой") }
                        jsonPath("$.body.errorMeetings[0].errorDescription") { value("Не удалось создать встречу из-за непредвиденной ошибки") }
                    }
            }
        }

        @Test
        fun create_feature_async_controller_error_validation_test() {
            runTest {
                val response: FeatureModelListRsDto = objectMapper.readJsonResource("json/FeatureModelListRs.json")

                whenever(featureService.createFeatureAsync(any())).thenReturn(response)

                mockMvc.post("/v1/features/feature/async") {
                    header(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, UUID.randomUUID().toString())
                    header(Headers.SOURCE_SYSTEM, "example")
                    contentType = MediaType.APPLICATION_JSON
                    content = "{\"descriptions\":null}"
                }.andExpect {
                    status { isBadRequest() }

                    jsonPath("$.status") { value(false) }
                    jsonPath("$.error.code") { value("IP-002") }
                    jsonPath("$.error.title") { value("Ошибка валидации запроса или ответа") }
                    jsonPath("$.error.description") { value("descriptions must not be null") }
                }
            }
        }
    }

    @Nested
    inner class CreateFeatureControllerTest {

        @Test
        fun create_feature_controller_success_test() {
            whenever(featureService.createFeature(any())).thenReturn(
                FeatureModelRsDto().apply {
                    id = 1
                    description = "Важная встреча с клиентом"
                }
            )

            mockMvc.post("/v1/features/feature/create") {
                // Конфигурируем заголовки и тело запроса внутри понятного DSL-блока
                header(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, UUID.randomUUID().toString())
                header(Headers.SOURCE_SYSTEM, "example")
                contentType = MediaType.APPLICATION_JSON
                content = "{\"description\":\"Важная встреча с клиентом\"}"
            }.andExpect {
                status { isOk() }

                jsonPath("$.status") { value(true) }
                jsonPath("$.body.id") { value(1) }
                jsonPath("$.body.description") { value("Важная встреча с клиентом") }
            }
        }

        @Test
        fun create_feature_controller_error_validation_test() {
            whenever(featureService.createFeature(any())).thenReturn(
                FeatureModelRsDto().apply {
                    id = 1
                    description = "Важная встреча с клиентом"
                }
            )
            mockMvc.post("/v1/features/feature/create") {
                header(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, UUID.randomUUID().toString())
                header(Headers.SOURCE_SYSTEM, "example")
                contentType = MediaType.APPLICATION_JSON
                content = "{\"description\":null}"
            }.andExpect {
                status { isBadRequest() }

                jsonPath("$.status") { value(false) }
                jsonPath("$.error.code") { value("IP-002") }
                jsonPath("$.error.title") { value("Ошибка валидации запроса или ответа") }
                jsonPath("$.error.description") { value("description must not be null") }
            }
        }
    }
}