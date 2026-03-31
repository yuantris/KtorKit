package io.kernel.ktor.request

import io.kernel.ktor.cache.CacheConfig
import io.kernel.ktor.cache.CacheHandler
import io.kernel.ktor.cache.CacheResult
import io.kernel.ktor.interceptor.InterceptorChain
import io.kernel.ktor.response.ApiResponse
import io.kernel.ktor.response.ResponseMapper
import io.kernel.ktor.response.WrappedResponse
import io.kernel.ktor.util.md5
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * 请求方法
 */
enum class RequestMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
}

/**
 * 请求构建器
 * 使用 CacheHandler 减少缓存逻辑重复
 * 使用 InterceptorChain 统一管理拦截器
 */
class RequestBuilder internal constructor(
    private val client: HttpClient,
    private val baseUrl: String,
    private val responseMapper: ResponseMapper,
    private val cacheHandler: CacheHandler? = null,
    private val interceptorChain: InterceptorChain = InterceptorChain.create()
) {
    private var path: String = ""
    private var method: RequestMethod = RequestMethod.GET
    private val headers = mutableMapOf<String, String>()
    private val queryParameters = mutableMapOf<String, Any?>()
    private var body: Any? = null
    private var cacheConfig: CacheConfig = CacheConfig()
    private var timeout: Long? = null

    /**
     * 设置请求路径
     */
    fun path(path: String): RequestBuilder = apply { this.path = path }

    /**
     * 设置 GET 请求
     */
    fun get(): RequestBuilder = apply { this.method = RequestMethod.GET }

    /**
     * 设置 POST 请求
     */
    fun post(): RequestBuilder = apply { this.method = RequestMethod.POST }

    /**
     * 设置 PUT 请求
     */
    fun put(): RequestBuilder = apply { this.method = RequestMethod.PUT }

    /**
     * 设置 DELETE 请求
     */
    fun delete(): RequestBuilder = apply { this.method = RequestMethod.DELETE }

    /**
     * 设置 PATCH 请求
     */
    fun patch(): RequestBuilder = apply { this.method = RequestMethod.PATCH }

    /**
     * 设置请求方法
     */
    fun method(method: RequestMethod): RequestBuilder = apply { this.method = method }

    /**
     * 添加请求头
     */
    fun header(key: String, value: String): RequestBuilder = apply {
        headers[key] = value
    }

    /**
     * 批量添加请求头
     */
    fun headers(headers: Map<String, String>): RequestBuilder = apply {
        this.headers.putAll(headers)
    }

    /**
     * 添加查询参数
     */
    fun query(key: String, value: Any?): RequestBuilder = apply {
        queryParameters[key] = value
    }

    /**
     * 批量添加查询参数
     */
    fun queries(queries: Map<String, Any?>): RequestBuilder = apply {
        queryParameters.putAll(queries)
    }

    /**
     * 设置请求体
     */
    fun body(body: Any): RequestBuilder = apply { this.body = body }

    /**
     * 设置缓存配置
     */
    fun cache(config: CacheConfig): RequestBuilder = apply {
        this.cacheConfig = config
    }

    /**
     * 设置超时时间
     */
    fun timeout(millis: Long): RequestBuilder = apply {
        this.timeout = millis
    }

    /**
     * 执行请求并返回包装响应
     */
    suspend fun <T> executeWrapped(serializer: KSerializer<T>): ApiResponse<T> {
        val cacheKey = cacheConfig.key ?: generateCacheKey()

        // 先检查缓存
        if (cacheHandler?.shouldCheckCacheFirst(cacheConfig) == true) {
            when (val result = cacheHandler.getFromCache(cacheKey, cacheConfig, WrappedResponse.serializer(serializer))) {
                is CacheResult.Hit -> return responseMapper.map(result.data)
                is CacheResult.Error -> return responseMapper.mapError(result.exception)
                is CacheResult.Miss -> if (cacheConfig.strategy == io.kernel.ktor.cache.CacheStrategy.CACHE_ONLY) {
                    return ApiResponse.Error(-1, "Cache miss")
                }
            }
        }

        // 执行网络请求
        return try {
            val response = executeRequest()
            val bodyText = response.bodyAsText()
            val wrapped = Json.decodeFromString(WrappedResponse.serializer(serializer), bodyText)

            // 成功时保存到缓存
            if (wrapped.code == 0) {
                cacheHandler?.saveToCache(cacheKey, cacheConfig, wrapped, WrappedResponse.serializer(serializer))
            }

            responseMapper.map(wrapped)
        } catch (e: Exception) {
            // 网络失败时尝试从缓存恢复
            if (cacheHandler?.shouldRecoverFromCache(cacheConfig) == true) {
                when (val result = cacheHandler.recoverFromCache(cacheKey, cacheConfig, WrappedResponse.serializer(serializer))) {
                    is CacheResult.Hit -> return responseMapper.map(result.data)
                    else -> {}
                }
            }
            responseMapper.mapError(e)
        }
    }

    /**
     * 执行请求并返回直接响应
     */
    suspend fun <T> executeDirect(serializer: KSerializer<T>): ApiResponse<T> {
        val cacheKey = cacheConfig.key ?: generateCacheKey()

        // 先检查缓存
        if (cacheHandler?.shouldCheckCacheFirst(cacheConfig) == true) {
            when (val result = cacheHandler.getFromCache(cacheKey, cacheConfig, serializer)) {
                is CacheResult.Hit -> return responseMapper.mapDirect(result.data)
                is CacheResult.Error -> return responseMapper.mapError(result.exception)
                is CacheResult.Miss -> if (cacheConfig.strategy == io.kernel.ktor.cache.CacheStrategy.CACHE_ONLY) {
                    return ApiResponse.Error(-1, "Cache miss")
                }
            }
        }

        // 执行网络请求
        return try {
            val response = executeRequest()
            val bodyText = response.bodyAsText()
            val data = Json.decodeFromString(serializer, bodyText)

            // 保存到缓存
            cacheHandler?.saveToCache(cacheKey, cacheConfig, data, serializer)

            responseMapper.mapDirect(data)
        } catch (e: Exception) {
            // 网络失败时尝试从缓存恢复
            if (cacheHandler?.shouldRecoverFromCache(cacheConfig) == true) {
                when (val result = cacheHandler.recoverFromCache(cacheKey, cacheConfig, serializer)) {
                    is CacheResult.Hit -> return responseMapper.mapDirect(result.data)
                    else -> {}
                }
            }
            responseMapper.mapError(e)
        }
    }

    /**
     * 执行请求并返回原始响应
     */
    suspend fun execute(): HttpResponse {
        return executeRequest()
    }

    /**
     * 执行请求并返回字符串
     */
    suspend fun executeString(): ApiResponse<String> {
        val cacheKey = cacheConfig.key ?: generateCacheKey()

        // 先检查缓存
        if (cacheHandler?.shouldCheckCacheFirst(cacheConfig) == true) {
            val cached = cacheHandler.getFromCache(cacheKey, cacheConfig, kotlinx.serialization.serializer<String>())
            if (cached is CacheResult.Hit) {
                return ApiResponse.Success(cached.data)
            }
        }

        return try {
            val response = executeRequest()
            val bodyText = response.bodyAsText()

            // 保存到缓存
            cacheHandler?.saveToCache(cacheKey, cacheConfig, bodyText, kotlinx.serialization.serializer())

            ApiResponse.Success(bodyText)
        } catch (e: Exception) {
            // 网络失败时尝试从缓存恢复
            if (cacheHandler?.shouldRecoverFromCache(cacheConfig) == true) {
                val cached = cacheHandler.getFromCache(cacheKey, cacheConfig, kotlinx.serialization.serializer<String>())
                if (cached is CacheResult.Hit) {
                    return ApiResponse.Success(cached.data)
                }
            }
            responseMapper.mapError(e)
        }
    }

    /**
     * 生成缓存 key
     */
    private fun generateCacheKey(): String {
        val sb = StringBuilder()
        sb.append(method.name).append(":").append(baseUrl).append(path)
        if (queryParameters.isNotEmpty()) {
            sb.append("?")
            queryParameters.toSortedMap().forEach { (key, value) ->
                sb.append(key).append("=").append(value).append("&")
            }
        }
        return sb.toString().md5()
    }

    /**
     * 执行请求（集成拦截器链）
     */
    private suspend fun executeRequest(): HttpResponse {
        // 重置拦截器链
        interceptorChain.reset()

        // 执行请求拦截器
        if (!interceptorChain.proceedRequest(this)) {
            throw IllegalStateException("Request intercepted")
        }

        val url = if (path.startsWith("http")) path else baseUrl + path

        // 执行 HTTP 请求
        val response = client.request(url) {
            method = when (this@RequestBuilder.method) {
                RequestMethod.GET -> HttpMethod.Get
                RequestMethod.POST -> HttpMethod.Post
                RequestMethod.PUT -> HttpMethod.Put
                RequestMethod.DELETE -> HttpMethod.Delete
                RequestMethod.PATCH -> HttpMethod.Patch
                RequestMethod.HEAD -> HttpMethod.Head
                RequestMethod.OPTIONS -> HttpMethod.Options
            }

            // 设置请求头
            this@RequestBuilder.headers.forEach { (key, value) ->
                this.headers.append(key, value)
            }

            // 设置查询参数
            this@RequestBuilder.queryParameters.forEach { (key, value) ->
                value?.let { this.url.parameters.append(key, it.toString()) }
            }

            // 设置请求体
            body?.let { setBody(it) }

            // 设置超时
            timeout?.let {
                timeout {
                    requestTimeoutMillis = it
                }
            }
        }

        // 执行响应拦截器
        return interceptorChain.proceedResponse(response)
    }

    companion object {
        /**
         * 创建请求构建器
         */
        fun create(
            client: HttpClient,
            baseUrl: String,
            responseMapper: ResponseMapper,
            cacheHandler: CacheHandler? = null,
            interceptorChain: InterceptorChain = InterceptorChain.create()
        ): RequestBuilder {
            return RequestBuilder(client, baseUrl, responseMapper, cacheHandler, interceptorChain)
        }
    }
}
