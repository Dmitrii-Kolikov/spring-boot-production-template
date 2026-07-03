package com.spring.boot.production.template.utils

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Utils {
    private val MOSCOW_ZONE = ZoneId.of("Europe/Moscow")
    private val RQ_TM_FORMATER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSxxx")

    fun rqTm(): String = OffsetDateTime.now(MOSCOW_ZONE).format(RQ_TM_FORMATER)
}