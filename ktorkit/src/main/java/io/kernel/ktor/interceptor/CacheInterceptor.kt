package io.kernel.ktor.interceptor

import io.kernel.ktor.cache.CacheConfig
import io.kernel.ktor.cache.CacheStrategy
import io.kernel.ktor.cache.DiskCacheManager
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*

/**
 * 缓存拦截器
 */
class CacheInterceptor(
    private val cacheManager: DiskCacheManager,
    private val defaultConfig: CacheConfig = CacheConfig()
) : RequestInterceptor {

    private val cacheConfigMap = mutableMapOf<String, CacheConfig>()

    /**
     * 设置特定请求的缓存配置
     */
    fun setCacheConfig(urlPattern: String, config: CacheConfig) {
        cacheConfigMap[urlPattern] = config
    }

    override suspend fun intercept(request: HttpRequestBuilder): Boolean {
        val url = request.url.buildString()
        val config = getConfigForUrl(url)

        if (!config.enabled || config.strategy == CacheStrategy.NO_CACHE) {
            return true
        }

        val cacheKey = config.key ?: generateCacheKey(request)

        // 对于缓存优先策略，检查是否有有效缓存
        if (config.strategy == CacheStrategy.CACHE_ONLY || config.strategy == CacheStrategy.CACHE_FIRST) {
            val cachedData = cacheManager.get(cacheKey)
            if (cachedData != null) {
                // 将缓存数据附加到请求属性中，供后续使用
                request.attributes.put(CacheKeys.CACHED_DATA_KEY, cachedData.data)
                request.attributes.put(CacheKeys.CACHE_HIT_KEY, true)
            } else if (config.strategy == CacheStrategy.CACHE_ONLY) {
                // 仅缓存模式且无缓存，标记为失败
                request.attributes.put(CacheKeys.CACHE_MISS_KEY, true)
            }
        }

        // 保存缓存 key 供响应用
        request.attributes.put(CacheKeys.CACHE_KEY_KEY, cacheKey)
        request.attributes.put(CacheKeys.CACHE_CONFIG_KEY, config)

        return true
    }

    /**
     * 保存响应到缓存
     */
    suspend fun saveToCache(request: HttpRequestBuilder, responseBody: String) {
        val cacheKey = request.attributes.getOrNull(CacheKeys.CACHE_KEY_KEY) ?: return
        val config = request.attributes.getOrNull(CacheKeys.CACHE_CONFIG_KEY) ?: defaultConfig

        if (!config.enabled) return

        val headers = mutableMapOf<String, String>()
        request.headers.entries().forEach { (key, values) ->
            headers[key] = values.joinToString()
        }

        cacheManager.put(
            key = cacheKey,
            data = responseBody,
            ttl = config.ttl,
            headers = headers
        )
    }

    /**
     * 获取缓存数据
     */
    suspend fun getCachedData(request: HttpRequestBuilder): String? {
        val cacheKey = request.attributes.getOrNull(CacheKeys.CACHE_KEY_KEY) ?: return null
        return cacheManager.getData(cacheKey)
    }

    /**
     * 清除缓存
     */
    suspend fun clearCache(key: String? = null) {
        if (key != null) {
            cacheManager.remove(key)
        } else {
            cacheManager.clear()
        }
    }

    private fun getConfigForUrl(url: String): CacheConfig {
        return cacheConfigMap.entries.firstOrNull { (pattern, _) ->
            url.contains(pattern, ignoreCase = true)
        }?.value ?: defaultConfig
    }

    private fun generateCacheKey(request: HttpRequestBuilder): String {
        val method = request.method.value
        val url = request.url.buildString()
        return "$method:$url".md5()
    }

    private fun String.md5(): String {
        val bytes = java.security.MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 缓存相关的请求属性 Key
     */
    object CacheKeys {
        val CACHE_KEY_KEY = AttributeKey<String>("cache_key")
        val CACHE_CONFIG_KEY = AttributeKey<CacheConfig>("cache_config")
        val CACHED_DATA_KEY = AttributeKey<String>("cached_data")
        val CACHE_HIT_KEY = AttributeKey<Boolean>("cache_hit")
        val CACHE_MISS_KEY = AttributeKey<Boolean>("cache_miss")
    }
}
