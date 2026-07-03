package com.spring.boot.production.template.config.logging.mask

import java.lang.annotation.Inherited

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class LogMask(
    val fields: Array<MaskField> = []
)