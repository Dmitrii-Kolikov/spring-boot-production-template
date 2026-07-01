package com.spring.boot.production.template.logging.db

import com.spring.boot.production.template.dictonary.Headers
import com.spring.boot.production.template.enums.ProductionError
import com.spring.boot.production.template.exception.DatabaseOperationException
import com.spring.boot.production.template.util.Utils
import jakarta.persistence.Table
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.GenericTypeResolver
import org.springframework.dao.DataAccessException
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Component
import java.sql.SQLException
import java.util.Optional
import java.util.UUID

@Aspect
@Component
class DbCompactLoggingListener(
    @Value("\${db.logging.enabled}") private val isDbLoggingEnabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(DbCompactLoggingListener::class.java)
    private val stringDbCallFormat = """
        
        ********************* DB Call *********************
         TABLE: {}
         METHOD: {}()
         EXECUTION_TIME: {} ms
         REST_ID: {}
         ROWS: {}
         STATUS: {}
        ***************************************************  
    """.trimIndent()

    private val stringDbCallFormatError = """
        
        ********************* DB Call *********************
         TABLE: {}
         METHOD: {}()
         EXECUTION_TIME: {} ms
         REST_ID: {}
         ERROR_MESSAGE: {}
         STATUS: {}
        ***************************************************  
    """.trimIndent()


    @Around("within(org.springframework.data.repository.Repository+)")
    fun logDbCall(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val uuid = MDC.get(Headers.REQUEST_CHAIN_ID_HTTP_HEADER) ?: UUID.randomUUID().toString()
        return try {
            val result = joinPoint.proceed()
            if (!isDbLoggingEnabled) { return result }
            val tableName = getTableName(joinPoint)
            val methodName = joinPoint.signature.name
            val executionTime = System.currentTimeMillis() - startTime
            val rowsCount = getRowsCount(result)
            log.info(stringDbCallFormat, tableName, methodName, executionTime, uuid, rowsCount, "SUCCESS")
            result
        } catch (throwable: Throwable) {
            val tableName = getTableName(joinPoint)
            val methodName = joinPoint.signature.name
            val executionTime = System.currentTimeMillis() - startTime
            val originalMessage = buildDetailedMessage(throwable)
            log.error(stringDbCallFormatError, tableName, methodName, executionTime, uuid, originalMessage, "ERROR")

            throw DatabaseOperationException(
                code = ProductionError.DATABASE_ERROR.code,
                title = ProductionError.DATABASE_ERROR.title,
                timestamp = Utils.rqTm(),
                rqUid = uuid,
                description = "Ошибка выполнения запроса к БД [$tableName] в методе $methodName() (время выполнения $executionTime мс). Ошибка: $originalMessage"
            )
        }
    }

    private fun getTableName(joinPoint: ProceedingJoinPoint): String {
        return try {
            // Находим ваш интерфейс репозитория, исключая системные пакеты Spring
            val repoInterface = joinPoint.target.javaClass.interfaces.firstOrNull {
                Repository::class.java.isAssignableFrom(it) && !it.name.startsWith("org.springframework.data")
            } ?: return "UNKNOWN"
            val types = GenericTypeResolver.resolveTypeArguments(repoInterface, Repository::class.java)
            val entityClass = types?.firstOrNull() ?: return "UNKNOWN"
            entityClass.getAnnotation(Table::class.java)?.name?.takeIf { it.isNotBlank() } ?: entityClass.simpleName
        } catch (ex: Exception) {
            "UNKNOWN" // Защита на случай, если что-то пойдет не так при рефлексии
        }
    }

    private fun getRowsCount(result: Any?): String {
        if (result == null) return "0"

        return when (result) {
            // Для методов, которые ничего не возвращают (flush, deleteById, void-процедуры)
            is Unit -> "—"

            // Если вернулся ByteArray — это всегда 1 бинарный объект (файл/картинка)
            is ByteArray -> "1"

            // Поддержка Page и Slice (пагинация Spring Data)
            is org.springframework.data.domain.Slice<*> -> "${result.content.size} (страница)"

            // Если вернулся Optional (стандарт для findById)
            is Optional<*> -> if (result.isPresent) "1" else "0"

            // Если вернулась коллекция (List, Set)
            is Collection<*> -> "${result.size} (список)"

            // Если вернулся массив
            is Array<*> -> "${result.size} (массив)"

            // Если метод возвращает Iterable (редко, но бывает)
            is Iterable<*> -> "${result.count()} (список)"

            // Если метод типа @Modifying (update/delete) возвращает количество измененных строк
            is Int -> "$result (изменено/удалено)"
            is Long -> "$result (изменено/удалено)"

            // Во всех остальных случаях вернулся 1 конкретный объект (Entity)
            else -> "1"
        }
    }

    private fun buildDetailedMessage(throwable: Throwable): String {
        // 1. Ищем SQLException
        val sqlException = generateSequence(throwable) { it.cause }
            .filterIsInstance<SQLException>()
            .lastOrNull()

        // 2. Извлекаем текст: из nextException для батчей, иначе из mostSpecificCause Спринга
        val rawMessage = when {
            sqlException?.nextException != null -> sqlException.nextException.message ?: ""
            throwable is DataAccessException -> throwable.mostSpecificCause.message ?: ""
            else -> throwable.message ?: "Неизвестная ошибка БД"
        }

        // 3. Срезаем технический префикс Hibernate, если он остался, и склеиваем строку в одну
        return when {
            rawMessage.contains("was aborted:") -> rawMessage.substringAfter("was aborted:").substringBefore("Call getNextException").trim()
            rawMessage.contains("Batch entry") -> rawMessage.substringAfterLast(":").trim()
            else -> rawMessage.trim()
        }.replace("\n", " ").replace("\\s+".toRegex(), " ")
    }
}