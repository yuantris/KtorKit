package io.kernel.ktor.cache

import android.content.Context
import io.kernel.ktor.util.md5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 磁盘缓存管理器
 */
class DiskCacheManager(
    context: Context,
    private val maxSize: Long = DEFAULT_MAX_SIZE,
    private val defaultTtl: Long = CacheConfig.DEFAULT_TTL,
    private val cacheDirName: String = DEFAULT_CACHE_DIR_NAME
) {
    companion object {
        const val DEFAULT_MAX_SIZE = 50 * 1024 * 1024L // 50MB
        const val DEFAULT_CACHE_DIR_NAME = "ktor_kit_cache"
    }

    private val cacheDir = File(context.cacheDir, cacheDirName)
    private val json = Json { ignoreUnknownKeys = true }

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * 获取缓存
     */
    suspend fun get(key: String): CacheEntry? = withContext(Dispatchers.IO) {
        try {
            val file = getCacheFile(key)
            if (!file.exists()) return@withContext null

            val entry = json.decodeFromString<CacheEntry>(file.readText())

            // 检查是否过期
            if (isExpired(entry)) {
                file.delete()
                return@withContext null
            }

            entry
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取缓存数据
     */
    suspend fun getData(key: String): String? {
        return get(key)?.data
    }

    /**
     * 保存缓存
     */
    suspend fun put(
        key: String,
        data: String,
        ttl: Long = defaultTtl,
        headers: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        try {
            val entry = CacheEntry(
                key = key,
                data = data,
                timestamp = System.currentTimeMillis(),
                ttl = ttl,
                headers = headers
            )

            val file = getCacheFile(key)
            file.writeText(json.encodeToString(entry))

            // 检查缓存大小
            evictIfNeeded()
        } catch (e: Exception) {
            // 缓存写入失败，忽略
        }
    }

    /**
     * 删除缓存
     */
    suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        try {
            val file = getCacheFile(key)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            // 删除失败，忽略
        }
    }

    /**
     * 清空所有缓存
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
        } catch (e: Exception) {
            // 清空失败，忽略
        }
    }

    /**
     * 清除过期缓存
     */
    suspend fun clearExpired() = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { file ->
                try {
                    val entry = json.decodeFromString<CacheEntry>(file.readText())
                    if (isExpired(entry)) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    // 解析失败，删除文件
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // 清除失败，忽略
        }
    }

    /**
     * 获取缓存大小
     */
    suspend fun getSize(): Long = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * 获取缓存数量
     */
    suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.size ?: 0
    }

    private fun getCacheFile(key: String): File {
        val hashedKey = key.md5()
        return File(cacheDir, hashedKey)
    }

    private fun isExpired(entry: CacheEntry): Boolean {
        return System.currentTimeMillis() - entry.timestamp > entry.ttl
    }

    private fun evictIfNeeded() {
        val files = cacheDir.listFiles() ?: return
        val totalSize = files.sumOf { it.length() }

        if (totalSize > maxSize) {
            // 按修改时间排序，删除最旧的文件
            files.sortedBy { it.lastModified() }.forEach { file ->
                if (cacheDir.listFiles()?.sumOf { it.length() } ?: 0 <= maxSize * 0.8) {
                    return
                }
                file.delete()
            }
        }
    }
}

/**
 * 缓存条目
 */
@Serializable
data class CacheEntry(
    val key: String,
    val data: String,
    val timestamp: Long,
    val ttl: Long,
    val headers: Map<String, String> = emptyMap()
)
