package com.spring.boot.production.template.logging.mask

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MaskField(
    val name: String,
    val type: MaskType
)