package com.spring.boot.production.template.config.logging.rest

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
}