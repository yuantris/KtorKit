package io.kernel.ktor.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SharedPreferences Token 存储实现
 */
class SharedPrefsTokenStorage(
    context: Context,
    private val prefsName: String = "ktor_kit_tokens"
) : TokenStorage {

    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
    }

    override suspend fun saveAccessToken(token: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    override suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    override suspend fun saveRefreshToken(token: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    override suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    override suspend fun saveTokenExpiry(expiry: Long) = withContext(Dispatchers.IO) {
        prefs.edit().putLong(KEY_TOKEN_EXPIRY, expiry).apply()
    }

    override suspend fun getTokenExpiry(): Long = withContext(Dispatchers.IO) {
        prefs.getLong(KEY_TOKEN_EXPIRY, 0)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
}

/**
 * 内存 Token 存储实现（用于测试）
 */
class MemoryTokenStorage : TokenStorage {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var tokenExpiry: Long = 0

    override suspend fun saveAccessToken(token: String) {
        accessToken = token
    }

    override suspend fun getAccessToken(): String? = accessToken

    override suspend fun saveRefreshToken(token: String) {
        refreshToken = token
    }

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun saveTokenExpiry(expiry: Long) {
        tokenExpiry = expiry
    }

    override suspend fun getTokenExpiry(): Long = tokenExpiry

    override suspend fun clear() {
        accessToken = null
        refreshToken = null
        tokenExpiry = 0
    }
}
