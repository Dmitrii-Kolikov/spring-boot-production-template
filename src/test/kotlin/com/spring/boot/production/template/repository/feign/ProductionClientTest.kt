package com.spring.boot.production.template.repository.feign

import com.spring.boot.production.template.api.dto.integration.FeatureIntegrationModelRqDto
import com.spring.boot.production.template.config.exception.feature.ProductionErrorDecoder
import com.spring.boot.production.template.config.header.Headers
import com.spring.boot.production.template.config.logging.fiegn.CustomFeignClientLogger
import com.spring.boot.production.template.utils.StubDecoderTest
import com.spring.boot.production.template.utils.StubEncoderTest
import com.spring.boot.production.template.utils.UtilsTest
import com.spring.boot.production.template.utils.UtilsTest.Companion.readJsonResource
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
        mockWebServer.enqueue(
            MockResponse()
                .setBody("{\"id\":10000000000166982,\"description\":\"Важная встреча с клиентом\"}")
                .setHeader("Content-Type", "application/json")
                .setHeader(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, UUID.randomUUID().toString())
                .setHeader(Headers.SOURCE_SYSTEM, "example")
                .setResponseCode(200)
        )

        val result = productionClient.getMeeting(10000000000166982L)
        assertNotNull(result)
        assertEquals(10000000000166982, result.id)
        assertEquals("Важная встреча с клиентом", result.description)
    }

    @Test
    fun create_meeting_repository_test() {
        val request: FeatureIntegrationModelRqDto = objectMapper.readJsonResource("json/FeatureIntegrationModelRq.json")

        mockWebServer.enqueue(
            MockResponse()
                .setBody("{\"id\":10000000000166982,\"description\":\"Важная встреча с клиентом\"}")
                .setHeader("Content-Type", "application/json")
                .setHeader(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, UUID.randomUUID().toString())
                .setHeader(Headers.SOURCE_SYSTEM, "example")
                .setResponseCode(200)
        )

        val result = productionClient.createMeeting(request)
        assertNotNull(result)
        assertEquals(10000000000166982, result.id)
        assertEquals("Важная встреча с клиентом", result.description)
    }
}