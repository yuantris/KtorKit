package io.kernel.ktor.interceptor

import io.ktor.client.request.*
import io.ktor.client.statement.*

/**
 * 请求拦截器接口
 */
interface RequestInterceptor {

    /**
     * 拦截请求
     * @param request 请求构建器
     * @return 是否继续执行
     */
    suspend fun intercept(request: HttpRequestBuilder): Boolean = true
}

/**
 * 响应拦截器接口
 */
interface ResponseInterceptor {

    /**
     * 拦截响应
     * @param response 响应
     * @return 处理后的响应
     */
    suspend fun intercept(response: HttpResponse): HttpResponse = response
}

/**
 * 拦截器链
 */
class InterceptorChain(
    private val interceptors: List<Any>,
    private var index: Int = 0
) {
    /**
     * 执行下一个拦截器
     */
    suspend fun proceed(request: HttpRequestBuilder): Boolean {
        while (index < interceptors.size) {
            val interceptor = interceptors[index++]
            when (interceptor) {
                is RequestInterceptor -> {
                    if (!interceptor.intercept(request)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    /**
     * 处理响应
     */
    suspend fun processResponse(response: HttpResponse): HttpResponse {
        var result = response
        interceptors.filterIsInstance<ResponseInterceptor>().forEach { interceptor ->
            result = interceptor.intercept(result)
        }
        return result
    }
}
