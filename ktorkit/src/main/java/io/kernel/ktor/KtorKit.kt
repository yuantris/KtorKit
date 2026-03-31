package io.kernel.ktor

import android.content.Context
import io.kernel.ktor.cache.CacheConfig
import io.kernel.ktor.cache.CacheHandler
import io.kernel.ktor.cache.DiskCacheManager
import io.kernel.ktor.client.HttpClientConfig
import io.kernel.ktor.client.HttpClientFactory
import io.kernel.ktor.interceptor.InterceptorChain
import io.kernel.ktor.interceptor.RequestInterceptor
import io.kernel.ktor.interceptor.ResponseInterceptor
import io.kernel.ktor.request.RequestBuilder
import io.kernel.ktor.response.ApiResponse
import io.kernel.ktor.response.DefaultResponseMapper
import io.kernel.ktor.response.ResponseMapper
import io.ktor.client.*
import kotlinx.serialization.serializer

/**
 * KtorKit 主入口
 * 提供统一的 HTTP 客户端访问接口
 */
class KtorKit private constructor(
    val client: HttpClient,
    val cacheManager: DiskCacheManager,
    val cacheHandler: CacheHandler,
    baseUrl: String,
    val responseMapper: ResponseMapper,
    val interceptorChain: InterceptorChain
) {
    /**
     * 基础 URL（可动态修改）
     */
    var baseUrl: String = baseUrl
        private set

    /**
     * 更新基础 URL
     * 用于开发时切换环境（开发/测试/生产）
     */
    fun updateBaseUrl(url: String) {
        baseUrl = url
    }

    companion object {
        @Volatile
        private var instance: KtorKit? = null

        /**
         * 初始化 KtorKit
         * @param context Android Context
         * @param config 配置 DSL
         * @return KtorKit 实例
         */
        fun init(context: Context, config: KtorKitConfig.() -> Unit): KtorKit {
            return instance ?: synchronized(this) {
                instance ?: createInstance(context, config).also { instance = it }
            }
        }

        /**
         * 获取 KtorKit 实例
         */
        fun getInstance(): KtorKit {
            return instance ?: throw IllegalStateException(
                "KtorKit not initialized. Call KtorKit.init() first."
            )
        }

        /**
         * 检查是否已初始化
         */
        fun isInitialized(): Boolean = instance != null

        /**
         * 释放实例
         */
        fun release() {
            instance?.client?.close()
            instance = null
        }

        private fun createInstance(
            context: Context,
            config: KtorKitConfig.() -> Unit
        ): KtorKit {
            val builder = KtorKitConfig().apply(config)

            // 创建 HttpClient
            val client = HttpClientFactory.create {
                baseUrl = builder.baseUrl
                debug = builder.debug
                authProvider = builder.authProvider
                cacheDir = context.cacheDir.absolutePath
                requestTimeout = builder.requestTimeout
                connectTimeout = builder.connectTimeout
                socketTimeout = builder.socketTimeout
                defaultHeaders.putAll(builder.defaultHeaders)
                defaultParameters.putAll(builder.defaultParameters)
                enableLogging = builder.enableLogging
                logLevel = builder.logLevel
                retryConfig = builder.retryConfig
                userAgent = builder.userAgent
            }

            // 创建缓存管理器
            val cacheManager = DiskCacheManager(
                context = context,
                maxSize = builder.diskCacheSize,
                defaultTtl = builder.defaultCacheTtl,
                cacheDirName = builder.cacheDirName
            )

            // 创建缓存处理器
            val cacheHandler = CacheHandler(cacheManager)

            // 创建响应映射器
            val responseMapper = builder.responseMapper
                ?: DefaultResponseMapper(builder.successCode, builder.exceptionHandler)

            // 创建拦截器链
            val interceptorChain = InterceptorChain.create(
                requestInterceptors = builder.requestInterceptors,
                responseInterceptors = builder.responseInterceptors
            )

            return KtorKit(
                client = client,
                cacheManager = cacheManager,
                cacheHandler = cacheHandler,
                baseUrl = builder.baseUrl,
                responseMapper = responseMapper,
                interceptorChain = interceptorChain
            )
        }
    }

    /**
     * 创建请求构建器
     */
    fun request(): RequestBuilder {
        return RequestBuilder.create(client, baseUrl, responseMapper, cacheHandler, interceptorChain)
    }

    /**
     * GET 请求
     */
    suspend inline fun <reified T> get(
        path: String,
        wrapped: Boolean = true,
        cacheConfig: CacheConfig = CacheConfig(),
        headers: Map<String, String> = emptyMap(),
        queries: Map<String, Any?> = emptyMap()
    ): ApiResponse<T> {
        return request().apply {
            path(path).get().cache(cacheConfig)
            headers(headers)
            queries(queries)
        }.let { builder ->
            if (wrapped) builder.executeWrapped(serializer<T>())
            else builder.executeDirect(serializer<T>())
        }
    }

    /**
     * POST 请求
     */
    suspend inline fun <reified T> post(
        path: String,
        body: Any? = null,
        wrapped: Boolean = true,
        cacheConfig: CacheConfig = CacheConfig(),
        headers: Map<String, String> = emptyMap(),
        queries: Map<String, Any?> = emptyMap()
    ): ApiResponse<T> {
        return request().apply {
            path(path).post().cache(cacheConfig)
            body?.let { body(it) }
            headers(headers)
            queries(queries)
        }.let { builder ->
            if (wrapped) builder.executeWrapped(serializer<T>())
            else builder.executeDirect(serializer<T>())
        }
    }

    /**
     * PUT 请求
     */
    suspend inline fun <reified T> put(
        path: String,
        body: Any? = null,
        wrapped: Boolean = true,
        cacheConfig: CacheConfig = CacheConfig(),
        headers: Map<String, String> = emptyMap(),
        queries: Map<String, Any?> = emptyMap()
    ): ApiResponse<T> {
        return request().apply {
            path(path).put().cache(cacheConfig)
            body?.let { body(it) }
            headers(headers)
            queries(queries)
        }.let { builder ->
            if (wrapped) builder.executeWrapped(serializer<T>())
            else builder.executeDirect(serializer<T>())
        }
    }

    /**
     * DELETE 请求
     */
    suspend inline fun <reified T> delete(
        path: String,
        wrapped: Boolean = true,
        cacheConfig: CacheConfig = CacheConfig(),
        headers: Map<String, String> = emptyMap(),
        queries: Map<String, Any?> = emptyMap()
    ): ApiResponse<T> {
        return request().apply {
            path(path).delete().cache(cacheConfig)
            headers(headers)
            queries(queries)
        }.let { builder ->
            if (wrapped) builder.executeWrapped(serializer<T>())
            else builder.executeDirect(serializer<T>())
        }
    }

    /**
     * PATCH 请求
     */
    suspend inline fun <reified T> patch(
        path: String,
        body: Any? = null,
        wrapped: Boolean = true,
        cacheConfig: CacheConfig = CacheConfig(),
        headers: Map<String, String> = emptyMap(),
        queries: Map<String, Any?> = emptyMap()
    ): ApiResponse<T> {
        return request().apply {
            path(path).patch().cache(cacheConfig)
            body?.let { body(it) }
            headers(headers)
            queries(queries)
        }.let { builder ->
            if (wrapped) builder.executeWrapped(serializer<T>())
            else builder.executeDirect(serializer<T>())
        }
    }

    /**
     * 清除所有缓存
     */
    suspend fun clearCache() {
        cacheManager.clear()
    }

    /**
     * 清除指定 key 的缓存
     */
    suspend fun clearCache(key: String) {
        cacheManager.remove(key)
    }

    /**
     * 清除过期缓存
     */
    suspend fun clearExpiredCache() {
        cacheManager.clearExpired()
    }

    /**
     * 获取缓存大小
     */
    suspend fun getCacheSize(): Long {
        return cacheManager.getSize()
    }
}

/**
 * KtorKit 配置 (open class 支持扩展)
 */
open class KtorKitConfig {
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
     * 认证提供者
     */
    var authProvider: io.kernel.ktor.auth.AuthProvider? = null

    /**
     * 响应映射器
     */
    var responseMapper: ResponseMapper? = null

    /**
     * 异常处理器
     */
    var exceptionHandler: io.kernel.ktor.exception.ExceptionHandler =
        io.kernel.ktor.exception.DefaultExceptionHandler()

    /**
     * 成功状态码
     */
    var successCode: Int = 0

    /**
     * 磁盘缓存大小（字节）
     */
    var diskCacheSize: Long = 50 * 1024 * 1024L // 50MB

    /**
     * 默认缓存过期时间（毫秒）
     */
    var defaultCacheTtl: Long = 5 * 60 * 1000L // 5分钟

    /**
     * 缓存目录名称
     */
    var cacheDirName: String = "ktor_kit_cache"

    /**
     * 是否启用日志
     */
    var enableLogging: Boolean = true

    /**
     * 日志级别
     */
    var logLevel: io.kernel.ktor.interceptor.LogLevel = io.kernel.ktor.interceptor.LogLevel.HEADERS

    /**
     * 默认请求头
     */
    val defaultHeaders: MutableMap<String, String> = mutableMapOf()

    /**
     * 默认查询参数
     */
    val defaultParameters: MutableMap<String, Any?> = mutableMapOf()

    /**
     * 重试配置
     */
    var retryConfig: io.kernel.ktor.interceptor.RetryConfig = io.kernel.ktor.interceptor.RetryConfig.DEFAULT

    /**
     * User-Agent
     */
    var userAgent: String = "KtorKit/1.0"

    /**
     * 请求拦截器列表
     */
    val requestInterceptors: MutableList<RequestInterceptor> = mutableListOf()

    /**
     * 响应拦截器列表
     */
    val responseInterceptors: MutableList<ResponseInterceptor> = mutableListOf()

    /**
     * 添加默认请求头
     */
    open fun header(key: String, value: String): KtorKitConfig {
        defaultHeaders[key] = value
        return this
    }

    /**
     * 添加默认查询参数
     */
    open fun parameter(key: String, value: Any?): KtorKitConfig {
        defaultParameters[key] = value
        return this
    }

    /**
     * 添加请求拦截器
     */
    open fun addRequestInterceptor(interceptor: RequestInterceptor): KtorKitConfig {
        requestInterceptors.add(interceptor)
        return this
    }

    /**
     * 添加响应拦截器
     */
    open fun addResponseInterceptor(interceptor: ResponseInterceptor): KtorKitConfig {
        responseInterceptors.add(interceptor)
        return this
    }

    /**
     * 添加拦截器（自动识别类型）
     */
    open fun addInterceptor(interceptor: Any): KtorKitConfig {
        when (interceptor) {
            is RequestInterceptor -> requestInterceptors.add(interceptor)
            is ResponseInterceptor -> responseInterceptors.add(interceptor)
        }
        return this
    }
}

/**
 * KtorKit 初始化 DSL
 */
fun KtorKit(configure: KtorKitConfig.() -> Unit): KtorKitConfig {
    return KtorKitConfig().apply(configure)
}
