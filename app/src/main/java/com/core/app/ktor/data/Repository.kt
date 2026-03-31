package com.core.app.ktor.data

import io.kernel.ktor.KtorKit
import io.kernel.ktor.response.ApiResponse
import io.kernel.ktor.response.onError
import io.kernel.ktor.response.onSuccess

/**
 * 用户 Repository
 */
class UserRepository(private val ktorKit: KtorKit) {

    private val apiService = UserApiService(ktorKit)

    /**
     * 登录
     */
    suspend fun login(username: String, password: String): ApiResponse<LoginResponse> {
        return apiService.login(username, password)
            .onSuccess { response ->
                // 登录成功，保存 Token
                // 这里可以调用 TokenStorage 保存 Token
            }
            .onError { code, message, exception ->
                // 处理登录失败
                android.util.Log.e("UserRepository", "Login failed: $code - $message")
            }
    }

    /**
     * 获取用户信息
     */
    suspend fun getUser(userId: Int): ApiResponse<User> {
        return apiService.getUser(userId)
    }

    /**
     * 获取用户列表
     */
    suspend fun getUsers(page: Int = 1, pageSize: Int = 20): ApiResponse<List<User>> {
        return apiService.getUsers(page, pageSize)
    }

    /**
     * 更新用户信息
     */
    suspend fun updateUser(userId: Int, user: User): ApiResponse<User> {
        return apiService.updateUser(userId, user)
    }

    /**
     * 删除用户
     */
    suspend fun deleteUser(userId: Int): ApiResponse<Unit> {
        return apiService.deleteUser(userId)
    }
}

/**
 * 文章 Repository
 */
class ArticleRepository(private val ktorKit: KtorKit) {

    private val apiService = ArticleApiService(ktorKit)

    /**
     * 获取文章列表
     */
    suspend fun getArticles(page: Int = 1, pageSize: Int = 20): ApiResponse<List<Article>> {
        return apiService.getArticles(page, pageSize)
    }

    /**
     * 获取文章详情
     */
    suspend fun getArticle(articleId: Int): ApiResponse<Article> {
        return apiService.getArticle(articleId)
    }

    /**
     * 搜索文章
     */
    suspend fun searchArticles(keyword: String): ApiResponse<List<Article>> {
        return apiService.searchArticles(keyword)
    }
}
