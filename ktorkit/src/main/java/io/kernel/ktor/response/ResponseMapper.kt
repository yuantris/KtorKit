package io.kernel.ktor.response

import io.kernel.ktor.exception.DefaultExceptionHandler
import io.kernel.ktor.exception.ExceptionHandler

/**
 * 响应映射器接口
 */
interface ResponseMapper {

    /**
     * 映射包装响应
     */
    fun <T> map(response: WrappedResponse<T>): ApiResponse<T>

    /**
     * 映射直接响应
     */
    fun <T> mapDirect(data: T): ApiResponse<T>

    /**
     * 映射异常
     */
    fun mapError(throwable: Throwable): ApiResponse.Error
}

/**
 * 默认响应映射器
 */
class DefaultResponseMapper(
    private val successCode: Int = 0,
    private val exceptionHandler: ExceptionHandler = DefaultExceptionHandler()
) : ResponseMapper {

    override fun <T> map(response: WrappedResponse<T>): ApiResponse<T> {
        return if (response.code == successCode && response.data != null) {
            ApiResponse.Success(response.data)
        } else {
            ApiResponse.Error(
                code = response.code,
                message = response.message.ifEmpty { "Unknown error" }
            )
        }
    }

    override fun <T> mapDirect(data: T): ApiResponse<T> {
        return ApiResponse.Success(data)
    }

    override fun mapError(throwable: Throwable): ApiResponse.Error {
        return exceptionHandler.handleException(throwable)
    }
}

/**
 * 自定义响应映射器
 * 用于非标准响应格式
 */
class CustomResponseMapper(
    private val successCodeExtractor: (Any?) -> Int,
    private val messageExtractor: (Any?) -> String,
    private val dataExtractor: (Any?) -> Any?,
    private val successCode: Int = 0,
    private val exceptionHandler: ExceptionHandler = DefaultExceptionHandler()
) : ResponseMapper {

    override fun <T> map(response: WrappedResponse<T>): ApiResponse<T> {
        val code = successCodeExtractor(response)
        val message = messageExtractor(response)
        val data = dataExtractor(response)

        return if (code == successCode && data != null) {
            @Suppress("UNCHECKED_CAST")
            ApiResponse.Success(data as T)
        } else {
            ApiResponse.Error(code = code, message = message)
        }
    }

    override fun <T> mapDirect(data: T): ApiResponse<T> {
        return ApiResponse.Success(data)
    }

    override fun mapError(throwable: Throwable): ApiResponse.Error {
        return exceptionHandler.handleException(throwable)
    }
}
