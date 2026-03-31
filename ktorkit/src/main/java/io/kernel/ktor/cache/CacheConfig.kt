package io.kernel.ktor.cache

/**
 * 缓存策略
 */
enum class CacheStrategy {
    /**
     * 不使用缓存
     */
    NO_CACHE,

    /**
     * 仅使用缓存，无缓存时返回错误
     */
    CACHE_ONLY,

    /**
     * 仅使用网络，不读取缓存
     */
    NETWORK_ONLY,

    /**
     * 缓存优先：先返回缓存，同时请求网络更新缓存
     */
    CACHE_FIRST,

    /**
     * 网络优先：先请求网络，失败时返回缓存
     */
    NETWORK_FIRST,

    /**
     * 缓存过期后才请求网络
     */
    CACHE_UNTIL_EXPIRED
}

/**
 * 缓存配置
 */
data class CacheConfig(
    /**
     * 缓存策略
     */
    val strategy: CacheStrategy = CacheStrategy.NETWORK_FIRST,

    /**
     * 缓存过期时间（毫秒）
     */
    val ttl: Long = DEFAULT_TTL,

    /**
     * 自定义缓存 key（可选）
     */
    val key: String? = null,

    /**
     * 是否启用缓存
     */
    val enabled: Boolean = true
) {
    companion object {
        /**
         * 默认过期时间：5分钟
         */
        const val DEFAULT_TTL = 5 * 60 * 1000L

        /**
         * 1分钟
         */
        const val TTL_1_MINUTE = 60 * 1000L

        /**
         * 5分钟
         */
        const val TTL_5_MINUTES = 5 * 60 * 1000L

        /**
         * 10分钟
         */
        const val TTL_10_MINUTES = 10 * 60 * 1000L

        /**
         * 1小时
         */
        const val TTL_1_HOUR = 60 * 60 * 1000L

        /**
         * 1天
         */
        const val TTL_1_DAY = 24 * 60 * 60 * 1000L

        /**
         * 不缓存
         */
        val NO_CACHE = CacheConfig(strategy = CacheStrategy.NO_CACHE, enabled = false)

        /**
         * 仅使用网络
         */
        val NETWORK_ONLY = CacheConfig(strategy = CacheStrategy.NETWORK_ONLY, enabled = false)

        /**
         * 仅使用缓存
         */
        val CACHE_ONLY = CacheConfig(strategy = CacheStrategy.CACHE_ONLY)

        /**
         * 缓存优先
         */
        val CACHE_FIRST = CacheConfig(strategy = CacheStrategy.CACHE_FIRST)
    }
}
