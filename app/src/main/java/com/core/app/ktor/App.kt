package com.core.app.ktor

import android.app.Application
import io.kernel.ktor.KtorKit
import io.kernel.ktor.auth.SharedPrefsTokenStorage
import io.kernel.ktor.auth.impl.BearerAuthProvider
import io.kernel.ktor.cache.CacheConfig
import io.kernel.ktor.interceptor.LogLevel
import io.kernel.ktor.interceptor.RetryConfig

/**
 * Application 类
 * 用于初始化 KtorKit
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initKtorKit()
    }

    /**
     * 初始化 KtorKit
     */
    private fun initKtorKit() {
        KtorKit.init(this) {
            // 基础配置
            baseUrl = "https://api.example.com" // 替换为实际的 API 地址
            debug = true // 在生产环境中应该设置为 false

            // 超时配置
            requestTimeout = 30_000L
            connectTimeout = 10_000L
            socketTimeout = 30_000L

            // 认证配置
            authProvider = BearerAuthProvider(
                tokenStorage = SharedPrefsTokenStorage(this@App),
                autoRefresh = true
            )

            // 缓存配置
            diskCacheSize = 50 * 1024 * 1024L // 50MB
            defaultCacheTtl = 5 * 60 * 1000L  // 5分钟

            // 日志配置
            enableLogging = true
            logLevel = LogLevel.BODY

            // 重试配置
            retryConfig = RetryConfig(
                maxRetries = 3,
                initialDelay = 1000L,
                retryOnTimeout = true,
                retryOnServerError = true
            )

            // 默认请求头
            header("X-App-Version", "1.0.0")
            header("X-Platform", "Android")
            header("Accept-Language", "zh-CN")

            // 默认查询参数
            // parameter("app_key", "your_app_key")

            // User-Agent
            userAgent = "KtorKit/1.0.0 (Android)"
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // 释放 KtorKit
        KtorKit.release()
    }
}
