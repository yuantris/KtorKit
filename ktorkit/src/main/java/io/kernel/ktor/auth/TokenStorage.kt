package io.kernel.ktor.auth

/**
 * Token 存储接口
 */
interface TokenStorage {

    /**
     * 保存 Access Token
     */
    suspend fun saveAccessToken(token: String)

    /**
     * 获取 Access Token
     */
    suspend fun getAccessToken(): String?

    /**
     * 保存 Refresh Token
     */
    suspend fun saveRefreshToken(token: String)

    /**
     * 获取 Refresh Token
     */
    suspend fun getRefreshToken(): String?

    /**
     * 保存 Token 过期时间
     */
    suspend fun saveTokenExpiry(expiry: Long)

    /**
     * 获取 Token 过期时间
     */
    suspend fun getTokenExpiry(): Long

    /**
     * 判断 Token 是否过期
     */
    suspend fun isTokenExpired(): Boolean {
        val expiry = getTokenExpiry()
        return expiry <= 0 || System.currentTimeMillis() >= expiry
    }

    /**
     * 清除所有 Token
     */
    suspend fun clear()

    /**
     * 判断是否有 Token
     */
    suspend fun hasToken(): Boolean {
        return getAccessToken() != null
    }
}

/**
 * Token 数据
 */
data class TokenData(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresIn: Long = 0,
    val tokenType: String = "Bearer"
)
