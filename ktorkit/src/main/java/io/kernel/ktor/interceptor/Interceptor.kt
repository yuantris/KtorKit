package io.kernel.ktor.interceptor

import io.ktor.client.request.*
import io.ktor.client.statement.*

/**
 * 请求拦截器接口
 */
interface RequestInterceptor {

    /**
     * 拦截请求
     * @param request 可以是 HttpRequestBuilder 或 Any
     * @return 是否继续执行
     */
    suspend fun intercept(request: Any): Boolean = true
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
 * 统一管理请求和响应拦截器
 */
class InterceptorChain private constructor(
    private val requestInterceptors: List<RequestInterceptor>,
    private val responseInterceptors: List<ResponseInterceptor>
) {
    private var requestIndex = 0
    private var responseIndex = 0

    /**
     * 执行请求拦截器
     * @param request 请求对象 (HttpRequestBuilder)
     * @return 是否继续执行
     */
    suspend fun proceedRequest(request: Any): Boolean {
        while (requestIndex < requestInterceptors.size) {
            val interceptor = requestInterceptors[requestIndex++]
            if (!interceptor.intercept(request)) {
                return false
            }
        }
        return true
    }

    /**
     * 执行响应拦截器
     * @param response 原始响应
     * @return 处理后的响应
     */
    suspend fun proceedResponse(response: HttpResponse): HttpResponse {
        var result = response
        for (interceptor in responseInterceptors) {
            result = interceptor.intercept(result)
        }
        return result
    }

    /**
     * 重置拦截器链
     */
    fun reset() {
        requestIndex = 0
        responseIndex = 0
    }

    companion object {
        /**
         * 创建拦截器链
         */
        fun create(
            requestInterceptors: List<RequestInterceptor> = emptyList(),
            responseInterceptors: List<ResponseInterceptor> = emptyList()
        ): InterceptorChain {
            return InterceptorChain(requestInterceptors, responseInterceptors)
        }

        /**
         * 从混合拦截器列表创建
         */
        fun fromInterceptors(interceptors: List<Any>): InterceptorChain {
            val requestList = mutableListOf<RequestInterceptor>()
            val responseList = mutableListOf<ResponseInterceptor>()

            interceptors.forEach { interceptor ->
                when (interceptor) {
                    is RequestInterceptor -> requestList.add(interceptor)
                    is ResponseInterceptor -> responseList.add(interceptor)
                }
            }

            return InterceptorChain(requestList, responseList)
        }
    }
}
