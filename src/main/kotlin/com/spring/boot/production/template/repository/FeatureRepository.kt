package com.spring.boot.production.template.repository

import com.spring.boot.production.template.model.entity.FeatureEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FeatureRepository: JpaRepository<FeatureEntity, Long> {

    fun findByDescription(description: String): FeatureEntity?
}