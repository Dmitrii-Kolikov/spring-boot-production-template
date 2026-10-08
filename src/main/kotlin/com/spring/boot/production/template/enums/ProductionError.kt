package com.spring.boot.production.template.enums

enum class ProductionError(
    val code: String,
    val title: String,
) {
    TECHNICAL_ERROR("IP-001", "Техническая ошибка"),
    VALIDATION_ERROR("IP-002", "Ошибка валидации запроса или ответа"),
    HEADER_VALIDATION_ERROR("IP-003", "Не заполнены значения заголовка"),
    DATABASE_ERROR("IP-004", "При обращении к таблице %s возникла ошибка в БД"),
    ERR_CREATE_PRODUCTION("PROD-01", "При создании продукта сервиса произошла ошибка"),
    ERR_GET_PRODUCTION("PROD-02", "При получения продукта сервиса произошла ошибка")
}