package com.spring.boot.production.template.controller

import com.spring.boot.production.template.logging.mask.LogMask
import com.spring.boot.production.template.logging.mask.MaskField
import com.spring.boot.production.template.logging.mask.MaskType
import com.spring.boot.production.template.model.BaseResponse
import com.spring.boot.production.template.model.FeatureModel
import com.spring.boot.production.template.service.api.IFeatureService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/v1/features"], produces = [MediaType.APPLICATION_JSON_VALUE] )
class FeatureController(
    private val featureService: IFeatureService
) {

    @GetMapping(value = ["/feature"])
    @LogMask(
        fields = [
            MaskField(name = "\$.body.description", MaskType.DESCRIPTION),
        ]
    )
    fun getFeature(): BaseResponse<FeatureModel> {
        return BaseResponse(true, featureService.getFeature(), null)
    }

    @GetMapping(value = ["/feature/async"])
    @LogMask(
        fields = [
            MaskField(name = "\$.body.description", MaskType.DESCRIPTION),
        ]
    )
    suspend fun getFeatureAsync(): BaseResponse<FeatureModel> {
        return  BaseResponse(true, featureService.getFeatureAsync(), null)
    }
}