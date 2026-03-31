package io.kernel.ktor.interceptor

import io.kernel.ktor.auth.AuthProvider
import io.ktor.client.request.*

/**
 * 认证拦截器
 */
class AuthInterceptor(
    private val authProvider: AuthProvider,
    private val excludedPaths: List<String> = emptyList()
) : RequestInterceptor {

    override suspend fun intercept(request: HttpRequestBuilder): Boolean {
        val path = request.url.buildString()

        // 检查是否在排除列表中
        if (excludedPaths.any { path.contains(it) }) {
            return true
        }

        // 应用认证
        authProvider.applyAuth(request)
        return true
    }

    /**
     * 添加排除路径
     */
    fun excludePath(path: String): AuthInterceptor {
        return AuthInterceptor(authProvider, excludedPaths + path)
    }

    companion object {
        /**
         * 创建排除特定路径的拦截器
         */
        fun create(
            authProvider: AuthProvider,
            vararg excludedPaths: String
        ): AuthInterceptor {
            return AuthInterceptor(authProvider, excludedPaths.toList())
        }
    }
}

/**
 * 可选认证拦截器
 * 当认证信息存在时才添加
 */
class OptionalAuthInterceptor(
    private val authProvider: AuthProvider
) : RequestInterceptor {

    override suspend fun intercept(request: HttpRequestBuilder): Boolean {
        if (authProvider.isAuthenticated()) {
            authProvider.applyAuth(request)
        }
        return true
    }
}
