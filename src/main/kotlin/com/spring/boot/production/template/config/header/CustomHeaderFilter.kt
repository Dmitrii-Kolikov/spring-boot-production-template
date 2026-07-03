package com.spring.boot.production.template.config.header

import com.spring.boot.production.template.config.exception.UserHeaderException
import com.spring.boot.production.template.enums.ProductionError
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class CustomHeaderFilter(
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {

    companion object {
        private val SHOULD_NOT_FILTER_PATHS = setOf("/health", "/actuator")
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        try {
            val uuid = request.getHeader(Headers.REQUEST_CHAIN_ID_HTTP_HEADER)
            val sourceSystem = request.getHeader(Headers.SOURCE_SYSTEM)

            if (uuid == null) {
                throw UserHeaderException(ProductionError.HEADER_VALIDATION_ERROR.code, "${ProductionError.HEADER_VALIDATION_ERROR.title} ${Headers.REQUEST_CHAIN_ID_HTTP_HEADER}")
            }

            if (sourceSystem == null) {
                throw UserHeaderException(ProductionError.HEADER_VALIDATION_ERROR.code, "${ProductionError.HEADER_VALIDATION_ERROR.title} ${Headers.SOURCE_SYSTEM}")
            }

            MDC.clear()
            MDC.put(Headers.REQUEST_CHAIN_ID_HTTP_HEADER, uuid)
            MDC.put(Headers.SOURCE_SYSTEM, sourceSystem)

            filterChain.doFilter(request, response)
        } catch (ex: UserHeaderException) {
            resolver.resolveException(request, response, null, ex)
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return SHOULD_NOT_FILTER_PATHS.any { path.startsWith(it) }
    }
}