package io.kernel.ktor.interceptor

import android.util.Log
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * 日志拦截器配置
 */
data class LogConfig(
    val enabled: Boolean = true,
    val level: LogLevel = LogLevel.BODY,
    val tag: String = "KtorKit",
    val logHeaders: Boolean = true,
    val logBody: Boolean = true,
    val logResponse: Boolean = true,
    val maxBodyLength: Int = 4096
)

/**
 * 日志级别
 */
enum class LogLevel {
    NONE,      // 不记录
    BASIC,     // 仅记录请求方法和URL
    HEADERS,   // 记录请求方法、URL和请求头
    BODY       // 记录所有信息包括请求体
}

/**
 * 日志拦截器
 */
class LoggingInterceptor(
    private val config: LogConfig = LogConfig()
) : RequestInterceptor, ResponseInterceptor {

    override suspend fun intercept(request: Any): Boolean {
        if (!config.enabled) return true
        if (request !is HttpRequestBuilder) return true

        val sb = StringBuilder()
        sb.appendLine("┌─────────────────────────────────────────────────────────")
        sb.appendLine("│ Request: ${request.method.value} ${request.url.buildString()}")
        sb.appendLine("├─────────────────────────────────────────────────────────")

        if (config.level >= LogLevel.HEADERS && config.logHeaders) {
            sb.appendLine("│ Headers:")
            request.headers.entries().forEach { (key, values) ->
                sb.appendLine("│   $key: ${values.joinToString()}")
            }
        }

        if (config.level >= LogLevel.BODY && config.logBody) {
            sb.appendLine("│ Body: <request body>")
        }

        sb.appendLine("└─────────────────────────────────────────────────────────")

        Log.d(config.tag, sb.toString())
        return true
    }

    override suspend fun intercept(response: HttpResponse): HttpResponse {
        if (!config.enabled || !config.logResponse) return response

        val sb = StringBuilder()
        sb.appendLine("┌─────────────────────────────────────────────────────────")
        sb.appendLine("│ Response: ${response.status.value} ${response.status.description}")
        sb.appendLine("│ URL: ${response.call.request.url}")
        sb.appendLine("├─────────────────────────────────────────────────────────")

        if (config.level >= LogLevel.HEADERS) {
            sb.appendLine("│ Headers:")
            response.headers.entries().forEach { (key, values) ->
                sb.appendLine("│   $key: ${values.joinToString()}")
            }
        }

        if (config.level >= LogLevel.BODY) {
            try {
                val bodyText = response.bodyAsText()
                val truncatedBody = if (bodyText.length > config.maxBodyLength) {
                    bodyText.take(config.maxBodyLength) + "... (truncated)"
                } else {
                    bodyText
                }
                sb.appendLine("│ Body:")
                sb.appendLine("│   $truncatedBody")
            } catch (e: Exception) {
                sb.appendLine("│ Body: <Unable to read body>")
            }
        }

        sb.appendLine("└─────────────────────────────────────────────────────────")

        Log.d(config.tag, sb.toString())
        return response
    }
}
