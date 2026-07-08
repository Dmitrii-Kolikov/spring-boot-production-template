package com.spring.boot.production.template.repository.feign

import com.spring.boot.production.template.api.dto.integration.FeatureIntegrationModelRsDto
import com.spring.boot.production.template.config.exception.feature.ProductionErrorDecoder
import com.spring.boot.production.template.config.header.Headers
import com.spring.boot.production.template.config.logging.fiegn.CustomFeignClientLogger
import com.spring.boot.production.template.utils.StubDecoderTest
import com.spring.boot.production.template.utils.StubEncoderTest
import com.spring.boot.production.template.utils.UtilsTest
import feign.Feign
import feign.Logger
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cloud.openfeign.support.SpringMvcContract
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProductionClientTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var productionClient: ProductionClient
    private val objectMapper = UtilsTest.jacksonObjectMapperMock()

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        productionClient = Feign.builder()
            .contract(SpringMvcContract())
            .encoder(StubEncoderTest())
            .decoder(StubDecoderTest())
            .errorDecoder(ProductionErrorDecoder(objectMapper))
            .logger(CustomFeignClientLogger(objectMapper, true, true, true, 409600))
            .logLevel(Logger.Level.FULL)
            .target(ProductionClient::class.java, "http://localhost:${mockWebServer.port}")
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun get_meeting_repository_test() {
        val responseBody = FeatureIntegrationModelRsDto().apply {
            id = 1
            description = "Важная встреча с клиентом"
        }

        mockWebServer.enqueue(
            MockResponse()
                .setBody(objectMapper.writeValueAsString(responseBody))
                .setHeader("Content-Type", "application/json")
                .setHeader(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, UUID.randomUUID().toString())
                .setHeader(Headers.SOURCE_SYSTEM, "example")
                .setResponseCode(200)
        )

        val result = productionClient.getMeeting("1")
        assertNotNull(result)
        assertEquals(1, result.id)
        assertEquals("Важная встреча с клиентом", result.description)
    }
}