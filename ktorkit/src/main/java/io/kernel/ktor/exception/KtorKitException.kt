package io.kernel.ktor.exception

/**
 * API 异常基类
 */
sealed class KtorKitException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * 网络异常
     */
    data class NetworkException(
        override val message: String = "Network error",
        override val cause: Throwable? = null
    ) : KtorKitException(message, cause)

    /**
     * 服务器异常
     */
    data class ServerException(
        val code: Int,
        override val message: String = "Server error",
        val errorBody: String? = null
    ) : KtorKitException(message)

    /**
     * 认证异常
     */
    data class AuthException(
        override val message: String = "Authentication failed",
        override val cause: Throwable? = null
    ) : KtorKitException(message, cause)

    /**
     * 超时异常
     */
    data class TimeoutException(
        override val message: String = "Request timeout",
        override val cause: Throwable? = null
    ) : KtorKitException(message, cause)

    /**
     * 解析异常
     */
    data class ParseException(
        override val message: String = "Parse error",
        override val cause: Throwable? = null
    ) : KtorKitException(message, cause)

    /**
     * 缓存异常
     */
    data class CacheException(
        override val message: String = "Cache error",
        override val cause: Throwable? = null
    ) : KtorKitException(message, cause)

    /**
     * 未知异常
     */
    data class UnknownException(
        override val message: String = "Unknown error",
        override val cause: Throwable? = null
    ) : KtorKitException(message, cause)
}

/**
 * 错误映射器
 */
object ErrorMapper {

    fun map(throwable: Throwable): KtorKitException {
        return when (throwable) {
            is KtorKitException -> throwable
            is java.net.SocketTimeoutException,
            is java.net.UnknownHostException,
            is java.net.ConnectException -> KtorKitException.NetworkException(
                message = throwable.message ?: "Network error",
                cause = throwable
            )
            is kotlinx.serialization.SerializationException -> KtorKitException.ParseException(
                message = throwable.message ?: "Parse error",
                cause = throwable
            )
            is io.ktor.client.plugins.ResponseException -> KtorKitException.ServerException(
                code = throwable.response.status.value,
                message = throwable.message ?: "Server error"
            )
            else -> KtorKitException.UnknownException(
                message = throwable.message ?: "Unknown error",
                cause = throwable
            )
        }
    }

    fun map(code: Int, message: String): KtorKitException {
        return when (code) {
            401, 403 -> KtorKitException.AuthException(message)
            408 -> KtorKitException.TimeoutException(message)
            in 500..599 -> KtorKitException.ServerException(code, message)
            else -> KtorKitException.ServerException(code, message)
        }
    }
}
