package com.spring.boot.production.template.repository.feign

import com.spring.boot.production.template.api.dto.integration.FeatureIntegrationModelRqDto
import com.spring.boot.production.template.api.dto.integration.FeatureIntegrationModelRsDto
import com.spring.boot.production.template.config.client.ProductionClientConfig
import com.spring.boot.production.template.config.logging.fiegn.LoggerConfig
import com.spring.boot.production.template.config.logging.mask.LogMask
import com.spring.boot.production.template.config.logging.mask.MaskField
import com.spring.boot.production.template.config.logging.mask.MaskType
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "production.client", url = "http://localhost:8080", configuration = [LoggerConfig::class, ProductionClientConfig::class])
interface ProductionClient {

    @LogMask(
        fields = [
            MaskField(name = "\$.description", MaskType.DESCRIPTION),
        ]
    )
    @GetMapping("feature/{meetingId}/detail")
    fun getMeeting(@PathVariable("meetingId") meetingId: String): FeatureIntegrationModelRsDto

    @LogMask(
        fields = [
            MaskField(name = "\$.description", MaskType.DESCRIPTION),
        ]
    )
    @PostMapping("feature/create")
    fun createMeeting(@RequestBody rq: FeatureIntegrationModelRqDto): FeatureIntegrationModelRsDto
}
