package io.kernel.ktor.cache

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * 缓存处理器
 * 封装缓存读取和写入逻辑，减少重复代码
 */
class CacheHandler(
    private val cacheManager: DiskCacheManager,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    /**
     * 从缓存获取数据并反序列化
     */
    suspend fun <T> getFromCache(
        cacheKey: String,
        config: CacheConfig,
        serializer: KSerializer<T>
    ): CacheResult<T> {
        if (!config.enabled) return CacheResult.Miss

        return when (config.strategy) {
            CacheStrategy.CACHE_ONLY -> {
                readCache(cacheKey, serializer, required = true)
            }
            CacheStrategy.CACHE_FIRST, CacheStrategy.CACHE_UNTIL_EXPIRED -> {
                readCache(cacheKey, serializer, required = false)
            }
            else -> CacheResult.Miss
        }
    }

    /**
     * 保存数据到缓存
     */
    suspend fun <T> saveToCache(
        cacheKey: String,
        config: CacheConfig,
        data: T,
        serializer: KSerializer<T>
    ) {
        if (!config.enabled || config.strategy == CacheStrategy.NO_CACHE) return

        try {
            val jsonString = json.encodeToString(serializer, data)
            cacheManager.put(cacheKey, jsonString, config.ttl)
        } catch (e: Exception) {
            // 缓存写入失败，忽略
        }
    }

    /**
     * 网络失败时从缓存恢复
     */
    suspend fun <T> recoverFromCache(
        cacheKey: String,
        config: CacheConfig,
        serializer: KSerializer<T>
    ): CacheResult<T> {
        if (!config.enabled) return CacheResult.Miss

        return when (config.strategy) {
            CacheStrategy.NETWORK_FIRST, CacheStrategy.NETWORK_ONLY -> {
                readCache(cacheKey, serializer, required = false)
            }
            else -> CacheResult.Miss
        }
    }

    /**
     * 判断是否应该先检查缓存
     */
    fun shouldCheckCacheFirst(config: CacheConfig): Boolean {
        return config.enabled && when (config.strategy) {
            CacheStrategy.CACHE_ONLY,
            CacheStrategy.CACHE_FIRST,
            CacheStrategy.CACHE_UNTIL_EXPIRED -> true
            else -> false
        }
    }

    /**
     * 判断网络失败时是否应该尝试缓存
     */
    fun shouldRecoverFromCache(config: CacheConfig): Boolean {
        return config.enabled && when (config.strategy) {
            CacheStrategy.NETWORK_FIRST,
            CacheStrategy.NETWORK_ONLY -> true
            else -> false
        }
    }

    private suspend fun <T> readCache(
        cacheKey: String,
        serializer: KSerializer<T>,
        required: Boolean
    ): CacheResult<T> {
        return try {
            val cached = if (required) {
                cacheManager.get(cacheKey)
            } else {
                cacheManager.get(cacheKey) // get 已经检查过期
            }

            if (cached != null) {
                val data = json.decodeFromString(serializer, cached.data)
                CacheResult.Hit(data)
            } else {
                CacheResult.Miss
            }
        } catch (e: Exception) {
            CacheResult.Error(e)
        }
    }
}

/**
 * 缓存结果密封类
 */
sealed class CacheResult<out T> {
    data class Hit<T>(val data: T) : CacheResult<T>()
    data object Miss : CacheResult<Nothing>()
    data class Error(val exception: Throwable) : CacheResult<Nothing>()

    val isHit: Boolean get() = this is Hit
    val isMiss: Boolean get() = this is Miss
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Hit -> data
        else -> null
    }
}
