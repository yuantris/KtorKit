package com.core.app.ktor.data

import io.kernel.ktor.KtorKit
import io.kernel.ktor.cache.CacheConfig
import io.kernel.ktor.cache.CacheStrategy
import io.kernel.ktor.response.ApiResponse

/**
 * 用户 API 服务
 */
class UserApiService(private val ktorKit: KtorKit) {

    /**
     * 登录（直接响应格式）
     */
    suspend fun login(username: String, password: String): ApiResponse<LoginResponse> {
        return ktorKit.post(
            path = "/api/auth/login",
            body = LoginRequest(username, password),
            wrapped = false // 直接响应格式
        )
    }

    /**
     * 获取用户信息（包装响应格式）
     */
    suspend fun getUser(userId: Int): ApiResponse<User> {
        return ktorKit.get(
            path = "/api/users/$userId",
            wrapped = true, // 包装响应格式
            cacheConfig = CacheConfig(
                strategy = CacheStrategy.CACHE_FIRST,
                ttl = CacheConfig.TTL_10_MINUTES
            )
        )
    }

    /**
     * 获取用户列表
     */
    suspend fun getUsers(page: Int = 1, pageSize: Int = 20): ApiResponse<List<User>> {
        return ktorKit.get(
            path = "/api/users",
            wrapped = true,
            queries = mapOf(
                "page" to page,
                "pageSize" to pageSize
            )
        )
    }

    /**
     * 更新用户信息
     */
    suspend fun updateUser(userId: Int, user: User): ApiResponse<User> {
        return ktorKit.put(
            path = "/api/users/$userId",
            body = user,
            wrapped = true
        )
    }

    /**
     * 删除用户
     */
    suspend fun deleteUser(userId: Int): ApiResponse<Unit> {
        return ktorKit.delete(
            path = "/api/users/$userId",
            wrapped = true
        )
    }
}

/**
 * 文章 API 服务
 */
class ArticleApiService(private val ktorKit: KtorKit) {

    /**
     * 获取文章列表（带缓存）
     */
    suspend fun getArticles(page: Int = 1, pageSize: Int = 20): ApiResponse<List<Article>> {
        return ktorKit.get(
            path = "/api/articles",
            wrapped = true,
            cacheConfig = CacheConfig(
                strategy = CacheStrategy.CACHE_FIRST,
                ttl = CacheConfig.DEFAULT_TTL
            ),
            queries = mapOf(
                "page" to page,
                "pageSize" to pageSize
            )
        )
    }

    /**
     * 获取文章详情
     */
    suspend fun getArticle(articleId: Int): ApiResponse<Article> {
        return ktorKit.get(
            path = "/api/articles/$articleId",
            wrapped = true,
            cacheConfig = CacheConfig(
                strategy = CacheStrategy.NETWORK_FIRST,
                ttl = CacheConfig.TTL_1_HOUR
            )
        )
    }

    /**
     * 搜索文章
     */
    suspend fun searchArticles(keyword: String): ApiResponse<List<Article>> {
        return ktorKit.get(
            path = "/api/articles/search",
            wrapped = true,
            queries = mapOf("q" to keyword)
        )
    }
}
