package com.spring.boot.production.template.config.logging.rest

import com.spring.boot.production.template.config.header.Headers
import com.spring.boot.production.template.config.logging.mask.JsonStreamMasker
import com.spring.boot.production.template.config.logging.mask.LogMask
import com.spring.boot.production.template.config.logging.mask.MaskField
import jakarta.servlet.DispatcherType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.WebUtils
import kotlin.text.iterator

/**
 * Компонент автоматического логирования и маскирования входящих REST-запросов и исходящих ответов.
 *
 * **Спецификация работы:**
 * - **Асинхронность:** Корректно обрабатывает повторные проходы (Async Dispatch), включая Kotlin Coroutines,
 *   гарантируя однократное логирование запроса и ответа без дублирования данных.
 * - **Маскирование:** Определяет целевой метод контроллера через [RequestMappingHandlerMapping]
 *   и лениво маскирует конфиденциальные данные в JSON-теле на основе аннотации [LogMask].
 * - **Защита памяти (Файлы):** Автоматически распознает бинарный контент и файлы
 *   (multipart, octet-stream, изображения и др.) по их заголовкам Content-Type, предотвращая их
 *   буферизацию в памяти и логируя только метаданные.
 * - **Лимит безопасности (OOM-Protection):** Ограничивает максимальный размер логируемого
 *   текстового тела значением [restMaxLoggableSize] для гарантированной защиты от перегрузки памяти Heap.
 * - **Обработка ошибок:** Для корректного логирования тел ошибочных ответов (4xx/5xx) требует
 *   наличия в системе глобального обработчика исключений `org.springframework.web.bind.annotation.RestControllerAdvice`.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RestLoggingFilter(
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    @Value("\${rest.logging.enabled}") private val isRestLoggingEnabled: Boolean,
    @Value("\${rest.masking.enabled}") private val isRestMaskingEnabled: Boolean,
    @Value("\${rest.max.loggable.size}") private val restMaxLoggableSize: Int
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RestLoggingFilter::class.java)

    companion object {
        private const val MASKED_FIELDS_ATTRIBUTES = "MASKED_FIELDS_ATTRIBUTES"
        private const val START_TIME_ATTRIBUTE = "REST_LOGGING_START_TIME"
        private val SHOULD_NOT_FILTER_PATHS = setOf("/health", "/actuator")

        private val REQUEST_FORMAT = """
            
            **************** Rest request call ****************
             METHOD: {}
             URL: {}
             REQUEST HEADERS: {}
             REST_ID: {}
             REQUEST BODY: {}
            ***************************************************  
        """.trimIndent()

        private val RESPONSE_FORMAT = """
            
            **************** Rest response call ****************
             METHOD: {}
             URL: {}
             RESPONSE STATUS: {}
             RESPONSE HEADERS: {}
             REST_ID: {}
             EXECUTION TIME: {} ms
             RESPONSE BODY: {}
            **************************************************** 
        """.trimIndent()
    }

    // Разрешаем повторный проход фильтра, когда корутина вернула результат (или ошибку в Advice)
    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return SHOULD_NOT_FILTER_PATHS.any { path.startsWith(it) }
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        if (!isRestLoggingEnabled) {
            filterChain.doFilter(request, response)
            return
        }

        // Проверяем тип контента ЗАПРОСА на наличие файлов ДО создания оберток
        val requestContentType = request.contentType
        val isRequestFile = isFilterContentType(requestContentType)

        // Если идет загрузка файла — НЕ кэшируем inputStream в память
        val requestWrapper: HttpServletRequest = if (isRequestFile) {
            request
        } else {
            WebUtils.getNativeRequest(request, CachedBodyHttpServletRequest::class.java)
                ?: CachedBodyHttpServletRequest(request)
        }

        val responseWrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper::class.java)
            ?: ContentCachingResponseWrapper(response)

        try {
            // 1. Логируем запрос "ДО" выполнения только один раз — при первом входящем вызове.
            if (request.dispatcherType == DispatcherType.REQUEST) {

                // Фиксируем время старта только при первом (основном) проходе
                request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis())

                if (isRequestFile) {
                    logFileRequest(request, requestContentType)
                } else {
                    //Читаем аннотацию: присутствуют данные для маскирования.
                    val maskFields: Array<MaskField>? = getLogMaskAnnotation(request).takeIf { isRestMaskingEnabled }
                    request.setAttribute(MASKED_FIELDS_ATTRIBUTES, maskFields)
                    logRequest(requestWrapper, maskFields)
                }
            }
            filterChain.doFilter(requestWrapper, responseWrapper)
        } finally {
            if (!isAsyncDispatch(request) && request.isAsyncStarted) {
                // Ждем завершения асинхронного потока / корутины
            } else {
                // Считаем время выполнения
                val startTime = request.getAttribute(START_TIME_ATTRIBUTE) as? Long ?: System.currentTimeMillis()
                val executionTime = System.currentTimeMillis() - startTime

                // Проверяем тип контента ОТВЕТА
                val responseContentType = responseWrapper.contentType
                val isResponseFile = isFilterContentType(responseContentType)

                val responsePayload = if (isResponseFile) {
                    val contentLength = responseWrapper.getHeader("Content-Length") ?: "unknown"
                    "[INFO] Бинарный ответ / Файл скрыт из логов. Размер: $contentLength байт, тип: $responseContentType"
                } else {
                    @Suppress("UNCHECKED_CAST")
                    val maskFields = request.getAttribute(MASKED_FIELDS_ATTRIBUTES) as? Array<MaskField>
                    processPayload(responseWrapper.contentAsByteArray, maskFields)
                }

                // 4. Выталкиваем кэшированное тело и заголовки в Tomcat / сервлет-контейнер.
                responseWrapper.copyBodyToResponse()

                // 5. Передаем оригинальный ответ в логгер, чтобы прочитать заголовки после копирования.
                // Сюда мы попадаем либо при обычном синхронном запросе,
                // либо на втором проходе (ASYNC), когда корутина завершилась (успешно или через ExceptionHandler)
                logResponse(requestWrapper, responseWrapper, executionTime, responsePayload)
            }
        }
    }

    private fun logRequest(request: HttpServletRequest, maskFields: Array<MaskField>?) {
        val headers = request.headerNames.asIterator().asSequence().associateWith { request.getHeader(it).orEmpty() }
        // Безопасно достаем байты: если это наша обертка, берем cachedBody, иначе null (для безопасности)
        val bodyBytes = (request as? CachedBodyHttpServletRequest)?.cachedBody
        val requestPayload = processPayload(bodyBytes, maskFields)
        log.info(REQUEST_FORMAT, request.method, request.requestURI, headers, request.getHeader(Headers.REQUEST_CHAIN_ID_HTTP_HEADER), requestPayload)
    }

    private fun logFileRequest(request: HttpServletRequest, contentType: String?) {
        val headers = request.headerNames.asIterator().asSequence().associateWith { request.getHeader(it).orEmpty() }
        // Получаем размер входящего файла из заголовков запроса
        val fileSize = request.contentLengthLong
        val filePayload = "[INFO] Бинарный запрос / Файл скрыт из логов. Размер: $fileSize байт, тип: $contentType"
        log.info(REQUEST_FORMAT, request.method, request.requestURI, headers, request.getHeader(Headers.REQUEST_CHAIN_ID_HTTP_HEADER).orEmpty(), filePayload)
    }

    private fun logResponse(request: HttpServletRequest, response: ContentCachingResponseWrapper, executionTime: Long, body: String?) {
        val headers = response.headerNames.associateWith { response.getHeader(it).orEmpty() }
        log.info(RESPONSE_FORMAT, request.method, request.requestURI, response.status, headers, request.getHeader(Headers.REQUEST_CHAIN_ID_HTTP_HEADER), executionTime, body)
    }

    private fun processPayload(bytes: ByteArray?, maskFields: Array<MaskField>?): String? {
        if (bytes == null || bytes.isEmpty()) return null

        if (bytes.size > restMaxLoggableSize) {
            return "[WARNING] Тело скрыто. Размер (${bytes.size} байт) превышает установленный лимит безопасности системы"
        }

        val raw = String(bytes, Charsets.UTF_8)
        val cleanBody = cleanWhitespace(raw)
        if (cleanBody.isBlank()) return null
        return JsonStreamMasker.mask(cleanBody, maskFields)
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

    private fun getLogMaskAnnotation(request: HttpServletRequest): Array<MaskField>? {
        try {
            val handlerChain = requestMappingHandlerMapping.getHandler(request)
            val handlerMethod = handlerChain?.handler as? HandlerMethod
            return handlerMethod?.getMethodAnnotation(LogMask::class.java)?.fields
        } catch (ex: Exception) {
            log.warn("Не удалось получить аннотацию маскирования для запроса: {}", ex.message)
        }
        return null
    }
}