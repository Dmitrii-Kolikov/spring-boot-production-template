package com.spring.boot.production.template.config.logging.fiegn

import com.fasterxml.jackson.databind.ObjectMapper
import com.spring.boot.production.template.config.header.Headers
import com.spring.boot.production.template.config.logging.mask.JsonStreamMasker
import com.spring.boot.production.template.config.logging.mask.LogMask
import com.spring.boot.production.template.config.logging.mask.MaskField
import feign.Logger
import feign.Request
import feign.Response
import org.slf4j.LoggerFactory
import kotlin.text.iterator

/**
 * Компонент автоматического логирования и маскирования исходящих интеграционных запросов и входящих ответов Feign.
 *
 * **Спецификация работы:**
 * - **Атомарность вызова:** Объединяет логирование отправленного запроса и полученного ответа
 *   в один общий блок логов, что упрощает сквозную трассировку интеграционного вызова.
 * - **Маскирование:** Напрямую извлекает метаданные метода Feign-клиента и лениво маскирует
 *   конфиденциальные данные в JSON-теле на основе аннотации [LogMask].
 * - **Фильтрация DTO (Short Logs):** При включенном флаге [isFeignShortLogs] автоматически
 *   десериализует и очищает JSON ответа от лишних полей, не описанных в возвращаемом типе (DTO) метода.
 * - **Защита памяти (Файлы):** Автоматически распознает бинарный контент и файлы
 *   (multipart, octet-stream, изображения и др.) по их заголовкам Content-Type, предотвращая их
 *   вычитку в память Heap и логируя только метаданные.
 * - **Лимит безопасности (OOM-Protection):** Ограничивает максимальный размер логируемого
 *   текстового тела значением [feignMaxLoggableSize] для гарантированной защиты от перегрузки памяти.
 *
 * *Важное требование:* Для корректной работы механизма Short Logs в конфигурации Jackson
 * должен быть отключен флаг строгого соответствия:
 * `spring.jackson.deserialization.fail-on-unknown-properties=false`
 */
class CustomFeignClientLogger(
    private val objectMapper: ObjectMapper,
    private val isFeignLoggingEnabled: Boolean,
    private val isFeignMaskingEnabled: Boolean,
    private val isFeignShortLogs: Boolean,
    private val feignMaxLoggableSize: Int
): Logger() {
    private val log = LoggerFactory.getLogger(CustomFeignClientLogger::class.java)

    companion object {
        private val INTEGRATION_FORMAT = """
            
            **************** Integration call *****************
             METHOD: {}
             URL: {}
             REQUEST HEADERS: {}
             REST_ID: {}
             REQUEST BODY: {}
             RESPONSE STATUS: {}
             RESPONSE HEADERS: {}
             RESPONSE BODY: {}
             EXECUTION TIME: {} ms
            ***************************************************  
        """.trimIndent()
    }

    override fun logRequest(configKey: String?, logLevel: Level?, request: Request?) {
        // Намеренно пусто. Логируем запрос и ответ вместе в методе logAndRebufferResponse
    }

    override fun logAndRebufferResponse(configKey: String?, logLevel: Level?, response: Response, elapsedTime: Long): Response {
        if (!isFeignLoggingEnabled) {
            return response
        }

        val request = response.request()

        // --- 1. БЕЗОПАСНАЯ ОБРАБОТКА ТЕЛА ЗАПРОСА ---
        var requestBody: String? = null
        val requestContentType = getHeaderValue(request.headers())

        if (isFilterContentType(requestContentType)) {
            val fileSize = request.body()?.size ?: 0
            requestBody = "[INFO] Бинарный запрос / Файл скрыт из логов. Размер: $fileSize байт, тип: $requestContentType"
        } else {
            val requestBodyBytes = request.body()
            if (requestBodyBytes != null && requestBodyBytes.isNotEmpty()) {
                if (requestBodyBytes.size > feignMaxLoggableSize) {
                    requestBody = "[WARNING] Тело запроса скрыто. Размер (${requestBodyBytes.size} байт) превышает установленный лимит безопасности системы"
                } else {
                    val maskFields = if (isFeignMaskingEnabled) getLogMaskAnnotation(request) else null
                    val rawRequest = String(requestBodyBytes, Charsets.UTF_8)
                    requestBody = JsonStreamMasker.mask(cleanWhitespace(rawRequest), maskFields)
                }
            }
        }

        // --- 2. БЕЗОПАСНАЯ ОБРАБОТКА ТЕЛА ОТВЕТА ---
        var bodyData: ByteArray? = null
        var responseBody: String? = null
        val responseContentType = getHeaderValue(response.headers())

        if (response.body() != null) {
            if (isFilterContentType(responseContentType)) {
                val contentLength = response.headers()["Content-Length"]?.firstOrNull() ?: "unknown"
                responseBody = "[INFO] Бинарный ответ / Файл скрыт из логов. Размер: $contentLength байт, тип: $responseContentType"
            } else {
                response.body().asInputStream().use { originalInputStream ->
                    bodyData = originalInputStream.readAllBytes()
                    val actualSize = bodyData?.size ?: 0

                    if (actualSize > feignMaxLoggableSize) {
                        responseBody = "[WARNING] Тело ответа скрыто. Размер ($actualSize байт) превышает установленный лимит безопасности системы"
                    } else if (actualSize > 0) {
                        val maskFields = if (isFeignMaskingEnabled) getLogMaskAnnotation(request) else null
                        val rawResponse = String(bodyData!!, Charsets.UTF_8)

                        val filteredJson = runCatching {
                            filterExtraFields(request, response, rawResponse, isFeignShortLogs)
                        }.getOrElse { rawResponse }

                        responseBody = JsonStreamMasker.mask(cleanWhitespace(filteredJson), maskFields)
                    }
                }
            }
        }

        // --- 3. ЗАПИСЬ В ЛОГ ---
        log.info(
            INTEGRATION_FORMAT,
            request.httpMethod().name,
            request.url(),
            request.headers(),
            request.headers()[Headers.REQUEST_CHAIN_ID_HTTP_HEADER]?.firstOrNull(),
            requestBody,
            response.status(),
            response.headers(),
            responseBody,
            elapsedTime
        )

        return bodyData?.let { response.toBuilder().body(it).build() } ?: response
    }

    override fun log(p0: String?, p1: String?, vararg p2: Any?) {
       // Преопределяем для Feign спецификации
    }

    private fun getHeaderValue(headers: Map<String, Collection<String>>?): String? {
        if (headers.isNullOrEmpty()) return null
        return headers.entries
            .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
            ?.value?.firstOrNull()
    }

    private fun cleanWhitespace(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (char in raw) {
            if (char == '\n' || char == '\r' || char == '\t') {
                if (sb.isNotEmpty() && sb.last() != ' ') {
                    sb.append(' ')
                }
            } else {
                sb.append(char)
            }
        }
        return sb.toString().trim()
    }

    private fun isFilterContentType(contentType: String?): Boolean {
        if (contentType == null) return false
        val ct = contentType.lowercase()
        return ct.contains("multipart/form-data") ||
                ct.contains("octet-stream") ||
                ct.contains("pdf") ||
                ct.startsWith("image/") ||
                ct.startsWith("audio/") ||
                ct.startsWith("video/")
    }

    private fun filterExtraFields(request: Request, response: Response, rawJson: String, isFeignShortLogs: Boolean): String {
        if (!isFeignShortLogs || response.status() !in 200..299) {
            return rawJson
        }

        return try {
            val returnType = request.requestTemplate()?.methodMetadata()?.returnType()
            if (returnType != null && returnType != Void.TYPE && returnType != String::class.java && returnType != Response::class.java) {
                val javaType = objectMapper.typeFactory.constructType(returnType)
                val dto = objectMapper.readValue<Any>(rawJson, javaType)
                objectMapper.writeValueAsString(dto)
            } else {
                rawJson
            }
        } catch (e : Exception) {
            log.warn(
                "Ошибка фильтрации Short Logs для метода {}: {}. Вывод оригинального JSON.",
                request.requestTemplate()?.methodMetadata()?.configKey() ?: "unknown",
                e.message
            )
            rawJson
        }
    }

    private fun getLogMaskAnnotation(request: Request?): Array<MaskField>? {
        return request?.requestTemplate()?.methodMetadata()?.method()
            ?.getAnnotation(LogMask::class.java)?.fields
    }
}