package com.spring.boot.production.template.controller

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
    fun getFeature(): String {
        return featureService.getFeature()
    }
}