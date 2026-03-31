package io.kernel.ktor.exception

import io.kernel.ktor.response.ApiResponse

/**
 * 异常处理器接口
 * 允许用户自定义异常处理逻辑
 */
interface ExceptionHandler {

    /**
     * 处理异常并返回 ApiResponse.Error
     */
    fun handleException(throwable: Throwable): ApiResponse.Error

    /**
     * 判断是否可以处理该异常
     */
    fun canHandle(throwable: Throwable): Boolean
}

/**
 * 默认异常处理器
 */
class DefaultExceptionHandler : ExceptionHandler {

    override fun handleException(throwable: Throwable): ApiResponse.Error {
        val exception = ErrorMapper.map(throwable)
        return when (exception) {
            is KtorKitException.ServerException -> ApiResponse.Error(
                code = exception.code,
                message = exception.message,
                exception = exception
            )
            is KtorKitException.AuthException -> ApiResponse.Error(
                code = 401,
                message = exception.message,
                exception = exception
            )
            is KtorKitException.TimeoutException -> ApiResponse.Error(
                code = 408,
                message = exception.message,
                exception = exception
            )
            is KtorKitException.NetworkException -> ApiResponse.Error(
                code = -1,
                message = exception.message,
                exception = exception
            )
            is KtorKitException.ParseException -> ApiResponse.Error(
                code = -2,
                message = exception.message,
                exception = exception
            )
            else -> ApiResponse.Error(
                code = -1,
                message = exception.message ?: "Unknown error",
                exception = exception
            )
        }
    }

    override fun canHandle(throwable: Throwable): Boolean = true
}

/**
 * 自定义异常处理器
 * 允许用户定义自己的异常处理规则
 */
class CustomExceptionHandler(
    private val handlers: List<ExceptionHandler> = emptyList(),
    private val fallback: ExceptionHandler = DefaultExceptionHandler()
) : ExceptionHandler {

    override fun handleException(throwable: Throwable): ApiResponse.Error {
        val handler = handlers.firstOrNull { it.canHandle(throwable) } ?: fallback
        return handler.handleException(throwable)
    }

    override fun canHandle(throwable: Throwable): Boolean {
        return handlers.any { it.canHandle(throwable) } || fallback.canHandle(throwable)
    }
}

/**
 * 异常处理器构建器
 */
class ExceptionHandlerBuilder {
    internal val handlerList = mutableListOf<ExceptionHandler>()
    private var fallback: ExceptionHandler = DefaultExceptionHandler()

    /**
     * 添加异常处理器
     */
    fun addHandler(handler: ExceptionHandler): ExceptionHandlerBuilder = apply {
        handlerList.add(handler)
    }

    /**
     * 添加特定异常的处理器
     */
    fun <T : Throwable> handle(
        exceptionClass: Class<T>,
        predicate: (T) -> Boolean = { true },
        handler: (T) -> ApiResponse.Error
    ): ExceptionHandlerBuilder = apply {
        handlerList.add(object : ExceptionHandler {
            override fun handleException(throwable: Throwable): ApiResponse.Error {
                @Suppress("UNCHECKED_CAST")
                return handler(throwable as T)
            }

            override fun canHandle(throwable: Throwable): Boolean {
                if (!exceptionClass.isInstance(throwable)) return false
                @Suppress("UNCHECKED_CAST")
                return predicate(throwable as T)
            }
        })
    }

    /**
     * 设置兜底处理器
     */
    fun fallback(handler: ExceptionHandler): ExceptionHandlerBuilder = apply {
        fallback = handler
    }

    /**
     * 构建异常处理器
     */
    fun build(): ExceptionHandler {
        return CustomExceptionHandler(handlerList, fallback)
    }
}

/**
 * 创建异常处理器的 DSL 函数
 */
fun buildExceptionHandler(configure: ExceptionHandlerBuilder.() -> Unit): ExceptionHandler {
    return ExceptionHandlerBuilder().apply(configure).build()
}
