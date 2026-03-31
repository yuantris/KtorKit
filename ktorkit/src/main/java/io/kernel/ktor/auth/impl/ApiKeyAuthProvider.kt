package io.kernel.ktor.auth.impl

import io.kernel.ktor.auth.AuthProvider
import io.kernel.ktor.auth.AuthType
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * API Key 认证提供者
 */
class ApiKeyAuthProvider(
    private val apiKey: String,
    private val headerName: String = "X-API-Key",
    private val location: ApiKeyLocation = ApiKeyLocation.HEADER
) : AuthProvider {

    override val type: AuthType = AuthType.API_KEY

    enum class ApiKeyLocation {
        HEADER,
        QUERY
    }

    override suspend fun applyAuth(request: HttpRequestBuilder) {
        when (location) {
            ApiKeyLocation.HEADER -> {
                request.headers.append(headerName, apiKey)
            }
            ApiKeyLocation.QUERY -> {
                request.parameter(headerName, apiKey)
            }
        }
    }

    override suspend fun refreshToken(): Boolean {
        // API Key 不支持刷新
        return false
    }

    override suspend fun isAuthenticated(): Boolean {
        return apiKey.isNotEmpty()
    }

    override suspend fun clearAuth() {
        // API Key 无法清除
    }
}

/**
 * 动态 API Key 认证提供者
 */
class DynamicApiKeyAuthProvider(
    private val apiKeyProvider: suspend () -> String,
    private val headerName: String = "X-API-Key",
    private val location: ApiKeyAuthProvider.ApiKeyLocation = ApiKeyAuthProvider.ApiKeyLocation.HEADER
) : AuthProvider {

    override val type: AuthType = AuthType.API_KEY

    override suspend fun applyAuth(request: HttpRequestBuilder) {
        val apiKey = apiKeyProvider()
        if (apiKey.isNotEmpty()) {
            when (location) {
                ApiKeyAuthProvider.ApiKeyLocation.HEADER -> {
                    request.headers.append(headerName, apiKey)
                }
                ApiKeyAuthProvider.ApiKeyLocation.QUERY -> {
                    request.parameter(headerName, apiKey)
                }
            }
        }
    }

    override suspend fun refreshToken(): Boolean = false

    override suspend fun isAuthenticated(): Boolean {
        return apiKeyProvider().isNotEmpty()
    }

    override suspend fun clearAuth() {}
}
