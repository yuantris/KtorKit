package io.kernel.ktor.interceptor

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay

/**
 * 重试配置
 */
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelay: Long = 1000L,       // 初始延迟（毫秒）
    val maxDelay: Long = 10000L,          // 最大延迟（毫秒）
    val backoffMultiplier: Double = 2.0,  // 退避乘数
    val retryOnTimeout: Boolean = true,
    val retryOnServerError: Boolean = true,
    val retryOnNetworkError: Boolean = true,
    val retryableStatusCodes: Set<Int> = setOf(408, 429, 500, 502, 503, 504)
) {
    companion object {
        val DEFAULT = RetryConfig()
        val NO_RETRY = RetryConfig(maxRetries = 0)
        val AGGRESSIVE = RetryConfig(
            maxRetries = 5,
            initialDelay = 500L,
            retryableStatusCodes = setOf(408, 429, 500, 502, 503, 504)
        )
    }
}

/**
 * 重试拦截器
 */
class RetryInterceptor(
    private val config: RetryConfig = RetryConfig.DEFAULT
) : RequestInterceptor {

    private var retryCount = 0

    override suspend fun intercept(request: HttpRequestBuilder): Boolean {
        retryCount = 0
        return true
    }

    /**
     * 判断是否应该重试
     */
    fun shouldRetry(throwable: Throwable): Boolean {
        if (retryCount >= config.maxRetries) return false

        return when (throwable) {
            is HttpRequestTimeoutException -> config.retryOnTimeout
            is io.ktor.client.network.sockets.ConnectTimeoutException -> config.retryOnTimeout
            is java.net.SocketTimeoutException -> config.retryOnTimeout
            is java.net.UnknownHostException -> config.retryOnNetworkError
            is java.net.ConnectException -> config.retryOnNetworkError
            is io.ktor.client.plugins.ResponseException -> {
                val statusCode = throwable.response.status.value
                config.retryableStatusCodes.contains(statusCode)
            }
            else -> false
        }
    }

    /**
     * 判断是否应该重试（基于状态码）
     */
    fun shouldRetry(statusCode: Int): Boolean {
        if (retryCount >= config.maxRetries) return false
        return config.retryableStatusCodes.contains(statusCode)
    }

    /**
     * 执行重试延迟
     */
    suspend fun onRetry() {
        retryCount++
        val delay = calculateDelay(retryCount)
        delay(delay)
    }

    /**
     * 计算延迟时间（指数退避）
     */
    private fun calculateDelay(retry: Int): Long {
        val delay = config.initialDelay * Math.pow(config.backoffMultiplier, (retry - 1).toDouble()).toLong()
        return minOf(delay, config.maxDelay)
    }

    /**
     * 重置重试计数
     */
    fun reset() {
        retryCount = 0
    }

    /**
     * 获取当前重试次数
     */
    fun getRetryCount(): Int = retryCount
}

/**
 * 带重试的请求执行器
 */
suspend fun <T> withRetry(
    config: RetryConfig = RetryConfig.DEFAULT,
    block: suspend () -> T
): T {
    val interceptor = RetryInterceptor(config)
    var lastException: Throwable? = null

    repeat(config.maxRetries + 1) { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e
            if (attempt < config.maxRetries && interceptor.shouldRetry(e)) {
                interceptor.onRetry()
            } else {
                throw e
            }
        }
    }

    throw lastException ?: IllegalStateException("Retry failed")
}
