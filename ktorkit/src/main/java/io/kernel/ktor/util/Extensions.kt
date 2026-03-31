package io.kernel.ktor.util

import io.kernel.ktor.response.ApiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.security.MessageDigest

/**
 * String 扩展函数
 */

/**
 * 计算 MD5 哈希
 */
fun String.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

/**
 * 计算 SHA256 哈希
 */
fun String.sha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

/**
 * 安全截断字符串
 */
fun String.truncate(maxLength: Int, suffix: String = "..."): String {
    return if (this.length <= maxLength) this
    else this.take(maxLength) + suffix
}

/**
 * URL 编码
 */
fun String.urlEncode(): String {
    return java.net.URLEncoder.encode(this, "UTF-8")
}

/**
 * URL 解码
 */
fun String.urlDecode(): String {
    return java.net.URLDecoder.decode(this, "UTF-8")
}

/**
 * Flow 扩展函数
 */

/**
 * 将 Flow 转换为 ApiResponse Flow
 */
fun <T> Flow<T>.asApiResponse(): Flow<ApiResponse<T>> {
    return this
        .map<T, ApiResponse<T>> { ApiResponse.Success(it) }
        .onStart { emit(ApiResponse.Loading) }
        .catch { emit(ApiResponse.Error(-1, it.message ?: "Unknown error")) }
}

/**
 * Map 扩展函数
 */

/**
 * 将 Map 转换为查询字符串
 */
fun Map<String, Any?>.toQueryString(): String {
    return this.filterValues { it != null }
        .entries
        .joinToString("&") { "${it.key.urlEncode()}=${it.value.toString().urlEncode()}" }
}

/**
 * 添加或更新 Map 中的值
 */
fun <K, V> MutableMap<K, V>.putIfAbsent(key: K, value: V) {
    if (!this.containsKey(key)) {
        this[key] = value
    }
}

/**
 * List 扩展函数
 */

/**
 * 安全获取列表元素
 */
fun <T> List<T>.getOrNull(index: Int): T? {
    return if (index in indices) this[index] else null
}

/**
 * 将列表分页
 */
fun <T> List<T>.paginate(page: Int, pageSize: Int): List<T> {
    val fromIndex = (page - 1) * pageSize
    val toIndex = minOf(fromIndex + pageSize, this.size)
    return if (fromIndex >= this.size) emptyList()
    else this.subList(fromIndex, toIndex)
}

/**
 * Any 扩展函数
 */

/**
 * 将对象转换为 Map
 */
fun Any.toMap(): Map<String, Any?> {
    return this::class.members
        .filter { it.parameters.size == 1 }
        .associate { it.name to it.call(this) }
}

/**
 * Throwable 扩展函数
 */

/**
 * 获取完整的异常堆栈信息
 */
fun Throwable.getStackTraceString(): String {
    val sb = StringBuilder()
    sb.appendLine(this.toString())
    this.stackTrace.forEach { element ->
        sb.appendLine("    at $element")
    }
    this.cause?.let { cause ->
        sb.appendLine("Caused by: ${cause.getStackTraceString()}")
    }
    return sb.toString()
}

/**
 * 检查是否是网络异常
 */
fun Throwable.isNetworkError(): Boolean {
    return this is java.net.UnknownHostException ||
            this is java.net.ConnectException ||
            this is java.net.SocketTimeoutException ||
            this is java.net.SocketException
}

/**
 * 检查是否是超时异常
 */
fun Throwable.isTimeoutError(): Boolean {
    return this is java.net.SocketTimeoutException ||
            this is io.ktor.client.plugins.HttpRequestTimeoutException ||
            this is io.ktor.client.network.sockets.ConnectTimeoutException
}
