package io.kernel.ktor.client

import io.kernel.ktor.auth.AuthProvider
import io.kernel.ktor.cache.CacheConfig
import io.kernel.ktor.interceptor.LogLevel
import io.kernel.ktor.interceptor.RetryConfig

/**
 * HttpClient 配置
 */
class HttpClientConfig {

    /**
     * 基础 URL
     */
    var baseUrl: String = ""

    /**
     * 是否开启调试模式
     */
    var debug: Boolean = false

    /**
     * 请求超时时间（毫秒）
     */
    var requestTimeout: Long = 30_000L

    /**
     * 连接超时时间（毫秒）
     */
    var connectTimeout: Long = 10_000L

    /**
     * Socket 超时时间（毫秒）
     */
    var socketTimeout: Long = 30_000L

    /**
     * HTTP 缓存大小（字节）
     */
    var httpCacheSize: Long = 10 * 1024 * 1024L // 10MB

    /**
     * 磁盘缓存目录
     */
    var cacheDir: String = ""

    /**
     * 认证提供者
     */
    var authProvider: AuthProvider? = null

    /**
     * 默认请求头
     */
    var defaultHeaders: MutableMap<String, String> = mutableMapOf()

    /**
     * 默认查询参数
     */
    var defaultParameters: MutableMap<String, Any?> = mutableMapOf()

    /**
     * 日志级别
     */
    var logLevel: LogLevel = if (debug) LogLevel.BODY else LogLevel.HEADERS

    /**
     * 重试配置
     */
    var retryConfig: RetryConfig = RetryConfig.DEFAULT

    /**
     * 默认缓存配置
     */
    var defaultCacheConfig: CacheConfig = CacheConfig()

    /**
     * 是否启用日志
     */
    var enableLogging: Boolean = true

    /**
     * 是否启用证书验证
     */
    var enableCertificatePinning: Boolean = false

    /**
     * 是否跟随重定向
     */
    var followRedirects: Boolean = true

    /**
     * User-Agent
     */
    var userAgent: String = "KtorKit/1.0"

    /**
     * 添加默认请求头
     */
    fun header(key: String, value: String): HttpClientConfig {
        defaultHeaders[key] = value
        return this
    }

    /**
     * 添加默认查询参数
     */
    fun parameter(key: String, value: Any?): HttpClientConfig {
        defaultParameters[key] = value
        return this
    }

    /**
     * 设置超时
     */
    fun timeout(
        request: Long = requestTimeout,
        connect: Long = connectTimeout,
        socket: Long = socketTimeout
    ): HttpClientConfig {
        requestTimeout = request
        connectTimeout = connect
        socketTimeout = socket
        return this
    }

    /**
     * 启用调试模式
     */
    fun enableDebug(): HttpClientConfig {
        debug = true
        logLevel = LogLevel.BODY
        enableLogging = true
        return this
    }

    /**
     * 构建配置
     */
    internal fun build(): HttpClientConfig {
        if (debug) {
            logLevel = LogLevel.BODY
            enableLogging = true
        }
        return this
    }
}

/**
 * HttpClient 配置 DSL
 */
fun HttpClientConfig(configure: HttpClientConfig.() -> Unit): HttpClientConfig {
    return HttpClientConfig().apply(configure).build()
}
