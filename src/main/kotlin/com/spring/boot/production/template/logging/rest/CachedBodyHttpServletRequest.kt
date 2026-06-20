package com.spring.boot.production.template.logging.rest

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

class CachedBodyHttpServletRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    // Сразу считываем всё тело запроса в память при создании объекта
    val cachedBody: ByteArray = request.inputStream.readAllBytes()

    override fun getInputStream(): ServletInputStream {
        val byteArrayInputStream = ByteArrayInputStream(cachedBody)

        return object : ServletInputStream() {
            override fun read(): Int = byteArrayInputStream.read()
            override fun isFinished(): Boolean = byteArrayInputStream.available() == 0
            override fun isReady(): Boolean = true
            override fun setReadListener(readListener: ReadListener?) {
                // Обычно для синхронных оберток здесь ничего делать не нужно
            }
        }
    }

    override fun getReader(): BufferedReader {
        val inputStream = this.inputStream
        return BufferedReader(InputStreamReader(inputStream, characterEncoding ?: "UTF-8"))
    }

    fun getBody(): String? {
        return cachedBody.takeIf { it.isNotEmpty() }?.let {
            val raw = String(cachedBody, charset(characterEncoding ?: "UTF-8"))
            val sb = StringBuilder(raw.length)

            raw.splitToSequence('\n').forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    if (sb.isNotEmpty()) sb.append(" ")
                    sb.append(trimmed)
                }
            }
            return sb.toString().takeIf { it.isNotBlank() }
        }
    }
}