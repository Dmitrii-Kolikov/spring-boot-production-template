package com.spring.boot.production.template.logging.rest

import com.spring.boot.production.template.logging.mask.JsonStreamMasker
import com.spring.boot.production.template.logging.mask.LogMask
import com.spring.boot.production.template.logging.mask.MaskField
import jakarta.servlet.DispatcherType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.WebUtils

@Component
class RestLoggingFilter(
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    @Value("\${rest.logging.enabled:false}") private val isRestLoggingEnabled: Boolean,
    @Value("\${rest.masking.enabled:false}") private val isRestMaskingEnabled: Boolean
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RestLoggingFilter::class.java)
    private val maskedFieldsAttributes = "MASKED_FIELDS_ATTRIBUTES"
    private val shouldNotFilterPaths = setOf("/health", "/actuator")
    private val stringRequestFormat = """
        
        **************** Rest request call ****************
         METHOD: {}
         URL: {}
         REQUEST HEADERS: {}
         REQUEST BODY: {}
        ***************************************************  
    """.trimIndent()

    private val stringResponseFormat = """
        
        **************** Rest response call ****************
         METHOD: {}
         URL: {}
         RESPONSE STATUS: {}
         RESPONSE HEADERS: {}
         RESPONSE BODY: {}
        **************************************************** 
    """.trimIndent()

    // Разрешаем повторный проход фильтра, когда корутина вернула результат (или ошибку в Advice)
    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return shouldNotFilterPaths.any { path.startsWith(it) }
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        if (!isRestLoggingEnabled) {
            filterChain.doFilter(request, response)
            return
        }

        val requestWrapper = WebUtils.getNativeRequest(request, CachedBodyHttpServletRequest::class.java)
            ?: CachedBodyHttpServletRequest(request)

        val responseWrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper::class.java)
            ?: ContentCachingResponseWrapper(response)

        try {
            // 1. Логируем запрос "ДО" выполнения только один раз — при первом входящем вызове.
            if (request.dispatcherType == DispatcherType.REQUEST) {
                //Читаем аннотацию: присутствуют данные для маскирования.
                val maskFields: Array<MaskField>? = getLogMaskAnnotation(request).takeIf { isRestMaskingEnabled }
                request.setAttribute(maskedFieldsAttributes, maskFields)
                logRequest(requestWrapper, maskFields)
            }
            filterChain.doFilter(requestWrapper, responseWrapper)
        } finally {
            // 2. Логируем ответ "ПОСЛЕ" выполнения
            // Если корутина только запустилась и ушла в фон — сейчас НЕ логируем, ждем её завершения.
            if (!isAsyncDispatch(request) && request.isAsyncStarted) {
                // Ничего не делаем, лог запишется, когда корутина вернет данные в поток ответа.
            } else {
                @Suppress("UNCHECKED_CAST")
                val maskFields: Array<MaskField>? = request.getAttribute(maskedFieldsAttributes) as? Array<MaskField>

                // 3. Читаем замаскированное тело DTO до сброса буфера обертки.
                val body = responseWrapper.contentAsByteArray
                    .takeIf { it.isNotEmpty() }
                    ?.let { JsonStreamMasker.mask(String(it, Charsets.UTF_8), maskFields) }

                // 4. Выталкиваем кэшированное тело и заголовки в Tomcat / сервлет-контейнер.
                responseWrapper.copyBodyToResponse()

                // 5. Передаем оригинальный ответ в логгер, чтобы прочитать заголовки после копирования.
                // Сюда мы попадаем либо при обычном синхронном запросе,
                // либо на втором проходе (ASYNC), когда корутина завершилась (успешно или через ExceptionHandler)
                logResponse(requestWrapper, responseWrapper, body)
            }
        }
    }

    private fun logRequest(request: CachedBodyHttpServletRequest, maskFields: Array<MaskField>?) {
        val headers = request.headerNames.asIterator().asSequence().associateWith { request.getHeader(it) }
        val rawBody = request.getBody()
        log.info(stringRequestFormat, request.method, request.requestURI, headers, JsonStreamMasker.mask(rawBody, maskFields))
    }

    private fun logResponse(request: CachedBodyHttpServletRequest, response: ContentCachingResponseWrapper, body: String?) {
        val headers = response.headerNames.associateWith { response.getHeader(it).orEmpty() }
        log.info(stringResponseFormat, request.method, request.requestURI, response.status, headers, body)
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