package io.kernel.ktor.auth.impl

import io.kernel.ktor.auth.AuthProvider
import io.kernel.ktor.auth.AuthType
import io.kernel.ktor.auth.TokenStorage
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * OAuth2 认证提供者
 */
class OAuth2AuthProvider(
    private val tokenStorage: TokenStorage,
    private val config: OAuth2Config,
    private val httpClient: HttpClient? = null
) : AuthProvider {

    override val type: AuthType = AuthType.OAUTH2

    data class OAuth2Config(
        val tokenUrl: String,
        val clientId: String,
        val clientSecret: String,
        val scope: String? = null,
        val refreshTokenUrl: String? = null
    )

    private val refreshMutex = Mutex()

    override suspend fun applyAuth(request: HttpRequestBuilder) {
        var token = tokenStorage.getAccessToken()

        // 检查是否需要刷新
        if (token != null && tokenStorage.isTokenExpired()) {
            refreshMutex.withLock {
                if (tokenStorage.isTokenExpired()) {
                    refreshToken()
                    token = tokenStorage.getAccessToken()
                }
            }
        }

        token?.let {
            request.headers.append(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    override suspend fun refreshToken(): Boolean {
        val client = httpClient ?: return false
        val refreshToken = tokenStorage.getRefreshToken() ?: return false

        return try {
            val url = config.refreshTokenUrl ?: config.tokenUrl
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    Parameters.build {
                        append("grant_type", "refresh_token")
                        append("refresh_token", refreshToken)
                        append("client_id", config.clientId)
                        append("client_secret", config.clientSecret)
                    }.formUrlEncode()
                )
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val json = Json.parseToJsonElement(body).jsonObject

                val accessToken = json["access_token"]?.jsonPrimitive?.content ?: return false
                val newRefreshToken = json["refresh_token"]?.jsonPrimitive?.content
                val expiresIn = json["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0

                tokenStorage.saveAccessToken(accessToken)
                newRefreshToken?.let { tokenStorage.saveRefreshToken(it) }
                if (expiresIn > 0) {
                    tokenStorage.saveTokenExpiry(System.currentTimeMillis() + expiresIn * 1000)
                }

                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun isAuthenticated(): Boolean {
        return tokenStorage.hasToken()
    }

    override suspend fun clearAuth() {
        tokenStorage.clear()
    }

    /**
     * 使用授权码获取 Token
     */
    suspend fun exchangeCodeForToken(code: String): Boolean {
        val client = httpClient ?: return false

        return try {
            val response: HttpResponse = client.post(config.tokenUrl) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    Parameters.build {
                        append("grant_type", "authorization_code")
                        append("code", code)
                        append("client_id", config.clientId)
                        append("client_secret", config.clientSecret)
                        config.scope?.let { append("scope", it) }
                    }.formUrlEncode()
                )
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val json = Json.parseToJsonElement(body).jsonObject

                val accessToken = json["access_token"]?.jsonPrimitive?.content ?: return false
                val refreshToken = json["refresh_token"]?.jsonPrimitive?.content
                val expiresIn = json["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0

                tokenStorage.saveAccessToken(accessToken)
                refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                if (expiresIn > 0) {
                    tokenStorage.saveTokenExpiry(System.currentTimeMillis() + expiresIn * 1000)
                }

                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 使用用户名密码获取 Token (Resource Owner Password Credentials)
     */
    suspend fun loginWithCredentials(username: String, password: String): Boolean {
        val client = httpClient ?: return false

        return try {
            val response: HttpResponse = client.post(config.tokenUrl) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    Parameters.build {
                        append("grant_type", "password")
                        append("username", username)
                        append("password", password)
                        append("client_id", config.clientId)
                        append("client_secret", config.clientSecret)
                        config.scope?.let { append("scope", it) }
                    }.formUrlEncode()
                )
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val json = Json.parseToJsonElement(body).jsonObject

                val accessToken = json["access_token"]?.jsonPrimitive?.content ?: return false
                val refreshToken = json["refresh_token"]?.jsonPrimitive?.content
                val expiresIn = json["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0

                tokenStorage.saveAccessToken(accessToken)
                refreshToken?.let { tokenStorage.saveRefreshToken(it) }
                if (expiresIn > 0) {
                    tokenStorage.saveTokenExpiry(System.currentTimeMillis() + expiresIn * 1000)
                }

                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
