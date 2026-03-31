package io.kernel.ktor.client

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.Cache
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * HttpClient 工厂
 */
object HttpClientFactory {

    /**
     * 创建 HttpClient
     */
    fun create(config: HttpClientConfig.() -> Unit): HttpClient {
        val builder = HttpClientConfig().apply(config).build()
        return create(builder)
    }

    /**
     * 创建 HttpClient
     */
    fun create(config: HttpClientConfig): HttpClient {
        return HttpClient(OkHttp) {
            // 配置序列化
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }

            // 配置日志
            if (config.enableLogging) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            android.util.Log.d("KtorKit", message)
                        }
                    }
                    level = when (config.logLevel) {
                        io.kernel.ktor.interceptor.LogLevel.NONE -> LogLevel.NONE
                        io.kernel.ktor.interceptor.LogLevel.BASIC -> LogLevel.INFO
                        io.kernel.ktor.interceptor.LogLevel.HEADERS -> LogLevel.HEADERS
                        io.kernel.ktor.interceptor.LogLevel.BODY -> LogLevel.BODY
                    }
                }
            }

            // 配置超时
            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeout
                connectTimeoutMillis = config.connectTimeout
                socketTimeoutMillis = config.socketTimeout
            }

            // 配置默认请求
            defaultRequest {
                // 设置基础 URL
                url(config.baseUrl)

                // 设置 Content-Type
                contentType(ContentType.Application.Json)

                // 设置 User-Agent
                headers.append(HttpHeaders.UserAgent, config.userAgent)

                // 添加默认请求头
                config.defaultHeaders.forEach { (key, value) ->
                    headers.append(key, value)
                }

                // 添加默认查询参数
                config.defaultParameters.forEach { (key, value) ->
                    value?.let { this.url.parameters.append(key, it.toString()) }
                }
            }

            // 配置 OkHttp 引擎
            engine {
                config {
                    // 配置缓存
                    if (config.cacheDir.isNotEmpty()) {
                        cache(Cache(File(config.cacheDir, "ktor_http_cache"), config.httpCacheSize))
                    }

                    // 配置超时
                    connectTimeout(config.connectTimeout, TimeUnit.MILLISECONDS)
                    readTimeout(config.socketTimeout, TimeUnit.MILLISECONDS)
                    writeTimeout(config.socketTimeout, TimeUnit.MILLISECONDS)

                    // 配置重定向
                    followRedirects(config.followRedirects)
                    followSslRedirects(config.followRedirects)
                }
            }

            // 配置 HttpResponseValidator
            HttpResponseValidator {
                validateResponse { response ->
                    val statusCode = response.status.value
                    when {
                        statusCode in 400..499 -> {
                            throw ClientRequestException(
                                response,
                                "Client error: $statusCode"
                            )
                        }
                        statusCode in 500..599 -> {
                            throw ServerResponseException(
                                response,
                                "Server error: $statusCode"
                            )
                        }
                    }
                }

                handleResponseExceptionWithRequest { cause, _ ->
                    // 可以在这里处理异常
                }
            }
        }
    }
}

/**
 * HttpClient 扩展函数
 */
fun HttpClient(configure: HttpClientConfig.() -> Unit): HttpClient {
    return HttpClientFactory.create(configure)
}
