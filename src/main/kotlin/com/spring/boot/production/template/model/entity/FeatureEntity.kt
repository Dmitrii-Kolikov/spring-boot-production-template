package com.spring.boot.production.template.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "FEATURE_TABLE")
class FeatureEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "meeting_id", nullable = false)
    var meetingId: Long? = null,

    @Column(name = "description", nullable = false)
    var description: String? = null
)