package com.spring.boot.production.template.enums

enum class ProductionError(
    val code: String,
    val title: String,
) {
    TECHNICAL_ERROR("IP-001", "Техническая ошибка"),
    VALIDATION_ERROR("IP-002", "Ошибка валидации запроса или ответа"),
    HEADER_VALIDATION_ERROR("IP-003", "Не заполнены значения заголовка"),
    DATABASE_ERROR("IP-004", "Ошибка выполнения запроса к БД")
}