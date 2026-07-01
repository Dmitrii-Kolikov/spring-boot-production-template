package com.spring.boot.production.template.controller

import com.spring.boot.production.template.api.dto.rest.FeatureModelRqDto
import com.spring.boot.production.template.api.dto.rest.FeatureModelRsDto
import com.spring.boot.production.template.logging.mask.LogMask
import com.spring.boot.production.template.logging.mask.MaskField
import com.spring.boot.production.template.logging.mask.MaskType
import com.spring.boot.production.template.model.BaseResponse
import com.spring.boot.production.template.service.api.IFeatureService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/v1/features"], produces = [MediaType.APPLICATION_JSON_VALUE] )
class FeatureController(
    private val featureService: IFeatureService
) {

    @GetMapping(value = ["/feature/{meetingId}"])
    @Valid
    @LogMask(
        fields = [
            MaskField(name = "\$.body.description", MaskType.DESCRIPTION),
        ]
    )
    fun getFeature(@PathVariable meetingId: Long): BaseResponse<FeatureModelRsDto> {
        return BaseResponse(true, featureService.getFeature(meetingId), null)
    }

    @GetMapping(value = ["/feature/async"])
    @Valid
    @LogMask(
        fields = [
            MaskField(name = "\$.body.description", MaskType.DESCRIPTION),
        ]
    )
    suspend fun getFeatureAsync(): BaseResponse<FeatureModelRsDto> {
        return  BaseResponse(true, featureService.getFeatureAsync(), null)
    }

    @PostMapping(value = ["/feature/create"])
    @Valid
    @LogMask(
        fields = [
            MaskField(name = "\$.body.description", MaskType.DESCRIPTION),
        ]
    )
    fun createFeature(@Valid @RequestBody rq: FeatureModelRqDto): BaseResponse<FeatureModelRsDto> {
        return BaseResponse(true, featureService.createFeature(rq), null)
    }
}