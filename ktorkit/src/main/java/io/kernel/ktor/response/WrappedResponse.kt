package io.kernel.ktor.response

import kotlinx.serialization.Serializable

/**
 * 包装响应数据类
 * 用于 { "code": 0, "message": "success", "data": {...} } 格式
 */
@Serializable
data class WrappedResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null
)

/**
 * 分页包装响应
 */
@Serializable
data class PagedResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: PagedData<T>? = null
)

@Serializable
data class PagedData<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val total: Long = 0,
    val hasMore: Boolean = false
)
