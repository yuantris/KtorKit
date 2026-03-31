package io.kernel.ktor.response

import io.kernel.ktor.exception.KtorKitException

/**
 * 统一 API 响应密封类
 */
sealed class ApiResponse<out T> {

    /**
     * 成功响应
     */
    data class Success<T>(val data: T) : ApiResponse<T>()

    /**
     * 错误响应
     */
    data class Error(
        val code: Int = -1,
        val message: String = "Unknown error",
        val exception: KtorKitException? = null
    ) : ApiResponse<Nothing>()

    /**
     * 加载中
     */
    data object Loading : ApiResponse<Nothing>()

    /**
     * 获取数据或 null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * 获取数据或默认值
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue
    }

    /**
     * 获取数据或抛出异常
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception ?: KtorKitException.UnknownException(message)
        is Loading -> throw IllegalStateException("Still loading")
    }

    /**
     * 判断是否成功
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * 判断是否失败
     */
    val isError: Boolean get() = this is Error

    /**
     * 判断是否加载中
     */
    val isLoading: Boolean get() = this is Loading
}

/**
 * 成功回调
 */
suspend inline fun <T> ApiResponse<T>.onSuccess(
    crossinline action: suspend (T) -> Unit
): ApiResponse<T> {
    if (this is ApiResponse.Success) action(data)
    return this
}

/**
 * 错误回调
 */
suspend inline fun <T> ApiResponse<T>.onError(
    crossinline action: suspend (code: Int, message: String, exception: KtorKitException?) -> Unit
): ApiResponse<T> {
    if (this is ApiResponse.Error) action(code, message, exception)
    return this
}

/**
 * 加载中回调
 */
suspend inline fun <T> ApiResponse<T>.onLoading(
    crossinline action: suspend () -> Unit
): ApiResponse<T> {
    if (this is ApiResponse.Loading) action()
    return this
}

/**
 * 映射转换
 */
inline fun <T, R> ApiResponse<T>.map(transform: (T) -> R): ApiResponse<R> = when (this) {
    is ApiResponse.Success -> ApiResponse.Success(transform(data))
    is ApiResponse.Error -> this
    is ApiResponse.Loading -> this
}

/**
 * 扁平映射
 */
suspend inline fun <T, R> ApiResponse<T>.flatMap(
    crossinline transform: suspend (T) -> ApiResponse<R>
): ApiResponse<R> = when (this) {
    is ApiResponse.Success -> transform(data)
    is ApiResponse.Error -> this
    is ApiResponse.Loading -> this
}
