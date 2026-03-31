package io.kernel.ktor.auth.impl

import io.kernel.ktor.auth.AuthProvider
import io.kernel.ktor.auth.AuthType
import io.kernel.ktor.auth.TokenRefreshCallback
import io.kernel.ktor.auth.TokenStorage
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Bearer Token 认证提供者
 */
class BearerAuthProvider(
    private val tokenStorage: TokenStorage,
    private val refreshCallback: TokenRefreshCallback? = null,
    private val autoRefresh: Boolean = true
) : AuthProvider {

    override val type: AuthType = AuthType.BEARER

    private val refreshMutex = Mutex()

    override suspend fun applyAuth(request: HttpRequestBuilder) {
        var token = tokenStorage.getAccessToken()

        // 检查是否需要刷新 Token
        if (autoRefresh && token != null && tokenStorage.isTokenExpired()) {
            refreshMutex.withLock {
                // 双重检查
                if (tokenStorage.isTokenExpired()) {
                    val refreshed = refreshToken()
                    if (refreshed) {
                        token = tokenStorage.getAccessToken()
                    }
                }
            }
        }

        token?.let {
            request.headers.append(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    override suspend fun refreshToken(): Boolean {
        if (refreshCallback == null) return false

        return try {
            val refreshToken = tokenStorage.getRefreshToken()
            val newToken = refreshCallback.refreshToken(refreshToken)

            if (newToken != null) {
                tokenStorage.saveAccessToken(newToken.accessToken)
                newToken.refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                if (newToken.expiresIn > 0) {
                    tokenStorage.saveTokenExpiry(System.currentTimeMillis() + newToken.expiresIn * 1000)
                }
                true
            } else {
                refreshCallback.onRefreshFailed(IllegalStateException("Token refresh returned null"))
                false
            }
        } catch (e: Exception) {
            refreshCallback.onRefreshFailed(e)
            false
        }
    }

    override suspend fun isAuthenticated(): Boolean {
        return tokenStorage.hasToken()
    }

    override suspend fun clearAuth() {
        tokenStorage.clear()
    }
}
