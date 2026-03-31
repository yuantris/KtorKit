package io.kernel.ktor.auth

import io.ktor.client.request.*

/**
 * 认证提供者接口
 */
interface AuthProvider {

    /**
     * 应用认证信息到请求
     */
    suspend fun applyAuth(request: HttpRequestBuilder)

    /**
     * 刷新 Token
     * @return 是否刷新成功
     */
    suspend fun refreshToken(): Boolean

    /**
     * 判断是否已认证
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * 清除认证信息
     */
    suspend fun clearAuth()

    /**
     * 认证类型
     */
    val type: AuthType
}

/**
 * 认证类型
 */
enum class AuthType {
    BEARER,
    API_KEY,
    OAUTH2,
    CUSTOM
}

/**
 * Token 刷新回调接口
 */
interface TokenRefreshCallback {

    /**
     * 刷新 Token
     * @param refreshToken 刷新 Token
     * @return 新的 Token 数据
     */
    suspend fun refreshToken(refreshToken: String?): TokenData?

    /**
     * Token 刷新失败回调
     */
    suspend fun onRefreshFailed(throwable: Throwable) {}
}
