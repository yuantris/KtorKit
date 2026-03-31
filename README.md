# KtorKit

一个适用于 Android 生产环境的 Ktor Client 封装库，提供简洁、类型安全的 API，内置缓存、认证、拦截器和错误处理支持。

[![](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![](https://img.shields.io/badge/Kotlin-2.0%2B-blue.svg)](https://kotlinlang.org/)
[![](https://img.shields.io/badge/Ktor-2.3.12-orange.svg)](https://ktor.io/)

[English](README_EN.md) | 中文

## 功能特性

- 🚀 **简洁 API** - 清晰直观的 DSL 网络请求
- 💾 **磁盘缓存** - 内置磁盘缓存，支持多种策略
- 🔐 **认证支持** - 支持 Bearer Token、API Key、OAuth2
- 🔄 **自动重试** - 可配置的指数退避重试
- 📝 **日志记录** - 多级别的请求/响应日志
- 🛡️ **类型安全** - 完整的 Kotlin Serialization 支持
- ⚡ **协程支持** - 原生协程支持
- 🎯 **拦截器链** - 请求和响应拦截器
- ❗ **错误处理** - 基于密封类的错误处理

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                    应用层 (Application)                  │
├─────────────────────────────────────────────────────────┤
│                    KtorKit API                          │
├─────────────────────────────────────────────────────────┤
│              RequestBuilder / CacheHandler              │
├─────────────────────────────────────────────────────────┤
│                    拦截器链 (Interceptor Chain)          │
│            (Auth → Logging → Retry → Cache)             │
├─────────────────────────────────────────────────────────┤
│                   HttpClient Core                       │
├─────────────────────────────────────────────────────────┤
│                     OkHttp 引擎                         │
└─────────────────────────────────────────────────────────┘
```

## 安装

### 步骤 1: 添加模块

将 `ktorkit` 模块复制到你的项目中，然后在 `settings.gradle` 添加：

```groovy
include ':ktorkit'
```

### 步骤 2: 添加依赖

在 app 的 `build.gradle` 中：

```groovy
dependencies {
    implementation project(':ktorkit')
}
```

## 快速开始

### 1. 在 Application 中初始化

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        KtorKit.init(this) {
            baseUrl = "https://api.example.com"
            debug = BuildConfig.DEBUG
            
            // 超时配置
            requestTimeout = 30_000L
            connectTimeout = 10_000L
            
            // 缓存配置
            diskCacheSize = 50 * 1024 * 1024L  // 50MB
            defaultCacheTtl = 5 * 60 * 1000L   // 5分钟
            
            // 默认请求头
            header("X-App-Version", BuildConfig.VERSION_NAME)
            header("Accept-Language", "zh-CN")
        }
    }
}
```

### 2. 发送请求

```kotlin
// 简单 GET 请求
val response = KtorKit.getInstance().get<User>("/users/1")

// POST 请求带请求体
val response = KtorKit.getInstance().post<User>(
    path = "/users",
    body = CreateUserRequest("张三", "zhangsan@example.com")
)

// 处理响应
response
    .onSuccess { user -> 
        // 处理成功
    }
    .onError { code, message, exception ->
        // 处理错误
    }
```

## 详细使用

### 响应格式

KtorKit 支持两种响应格式：

#### 包装响应格式（推荐）

```kotlin
// 服务器返回: { "code": 0, "message": "success", "data": {...} }
val response = ktorKit.get<User>("/users/1", wrapped = true)
```

#### 直接响应格式

```kotlin
// 服务器返回: { "id": 1, "name": "张三" }
val response = ktorKit.get<User>("/users/1", wrapped = false)
```

### 请求构建器

对于复杂请求，使用 RequestBuilder：

```kotlin
val response = KtorKit.getInstance().request()
    .path("/api/users")
    .get()
    .query("page", 1)
    .query("pageSize", 20)
    .header("X-Request-ID", UUID.randomUUID().toString())
    .cache(CacheConfig.CACHE_FIRST)
    .timeout(60_000L)
    .executeWrapped(serializer<User>())
```

### 缓存配置

KtorKit 提供磁盘缓存，支持多种策略：

```kotlin
// 缓存策略
CacheStrategy.NO_CACHE          // 不使用缓存
CacheStrategy.CACHE_ONLY        // 仅使用缓存，无缓存时返回错误
CacheStrategy.NETWORK_ONLY      // 仅使用网络
CacheStrategy.CACHE_FIRST       // 缓存优先，同时请求网络更新
CacheStrategy.NETWORK_FIRST     // 网络优先，失败时使用缓存
CacheStrategy.CACHE_UNTIL_EXPIRED  // 缓存未过期时使用缓存

// 使用示例
val response = ktorKit.get<User>(
    path = "/users/1",
    cacheConfig = CacheConfig(
        strategy = CacheStrategy.CACHE_FIRST,
        ttl = CacheConfig.TTL_10_MINUTES
    )
)

// 清除缓存
ktorKit.clearCache()
ktorKit.clearExpiredCache()
```

### 认证配置

#### Bearer Token

```kotlin
val tokenStorage = SharedPrefsTokenStorage(context)

KtorKit.init(this) {
    authProvider = BearerAuthProvider(
        tokenStorage = tokenStorage,
        refreshCallback = object : TokenRefreshCallback {
            override suspend fun refreshToken(refreshToken: String?): TokenData? {
                // 调用刷新 Token 接口
                return api.refreshToken(refreshToken)
            }
        },
        autoRefresh = true
    )
}
```

#### API Key

```kotlin
KtorKit.init(this) {
    authProvider = ApiKeyAuthProvider(
        apiKey = "your-api-key",
        headerName = "X-API-Key",
        location = ApiKeyAuthProvider.ApiKeyLocation.HEADER
    )
}
```

#### OAuth2

```kotlin
val tokenStorage = SharedPrefsTokenStorage(context)

KtorKit.init(this) {
    authProvider = OAuth2AuthProvider(
        tokenStorage = tokenStorage,
        config = OAuth2AuthProvider.OAuth2Config(
            tokenUrl = "https://oauth.example.com/token",
            clientId = "your-client-id",
            clientSecret = "your-client-secret"
        ),
        httpClient = HttpClient(OkHttp)
    )
}
```

### 动态更改 Base URL

在开发过程中，你可能需要在不同环境（开发/测试/生产）之间切换。KtorKit 提供了 `updateBaseUrl` 方法支持动态更改：

```kotlin
// 初始化后随时更改 baseUrl
KtorKit.getInstance().updateBaseUrl("https://test-api.example.com")
```

#### 在应用中实现环境切换

```kotlin
// 定义你的环境配置
data class ApiEnvironment(
    val name: String,
    val baseUrl: String
)

// 在你的应用中管理环境
object AppEnvironment {
    val DEV = ApiEnvironment("开发", "https://dev-api.example.com")
    val TEST = ApiEnvironment("测试", "https://test-api.example.com")
    val PROD = ApiEnvironment("生产", "https://api.example.com")
    
    fun getAll() = listOf(DEV, TEST, PROD)
    
    fun switch(env: ApiEnvironment) {
        KtorKit.getInstance().updateBaseUrl(env.baseUrl)
    }
}

// 在设置页面使用
fun showEnvironmentDialog(context: Context) {
    val environments = AppEnvironment.getAll()
    val names = environments.map { "${it.name}: ${it.baseUrl}" }.toTypedArray()
    
    AlertDialog.Builder(context)
        .setTitle("切换环境")
        .setItems(names) { _, which ->
            AppEnvironment.switch(environments[which])
        }
        .show()
}
```

### 拦截器

#### 日志拦截器

```kotlin
KtorKit.init(this) {
    addRequestInterceptor(LoggingInterceptor(
        LogConfig(
            enabled = true,
            level = LogLevel.BODY,
            tag = "MyApp",
            maxBodyLength = 2048
        )
    ))
}
```

#### 认证拦截器

```kotlin
KtorKit.init(this) {
    addRequestInterceptor(AuthInterceptor.create(
        authProvider = bearerAuthProvider,
        "/auth/login",      // 排除登录路径
        "/auth/register"    // 排除注册路径
    ))
}
```

#### 自定义拦截器

```kotlin
class HeaderInterceptor(
    private val headers: Map<String, String>
) : RequestInterceptor {
    override suspend fun intercept(request: Any): Boolean {
        if (request is HttpRequestBuilder) {
            headers.forEach { (key, value) ->
                request.headers.append(key, value)
            }
        }
        return true
    }
}

// 使用
KtorKit.init(this) {
    addRequestInterceptor(HeaderInterceptor(mapOf(
        "X-Custom-Header" to "value"
    )))
}
```

### 错误处理

```kotlin
val response = ktorKit.get<User>("/users/1")

when (response) {
    is ApiResponse.Success -> {
        val user = response.data
        // 处理成功
    }
    is ApiResponse.Error -> {
        val code = response.code
        val message = response.message
        val exception = response.exception
        // 处理错误
    }
    is ApiResponse.Loading -> {
        // 处理加载状态
    }
}

// 或使用扩展函数
response
    .onSuccess { user -> /* 处理成功 */ }
    .onError { code, message, exception -> /* 处理错误 */ }
```

#### 自定义异常处理器

```kotlin
KtorKit.init(this) {
    exceptionHandler = buildExceptionHandler {
        handle(java.net.SocketTimeoutException::class.java) { e ->
            ApiResponse.Error(code = -999, message = "请求超时，请重试")
        }
        handle(java.net.UnknownHostException::class.java) { e ->
            ApiResponse.Error(code = -998, message = "网络不可用")
        }
    }
}
```

### 网络监测

```kotlin
val monitor = NetworkMonitor.getInstance(context)

// 检查网络是否可用
if (monitor.isNetworkAvailable()) {
    // 网络可用
}

// 获取网络类型
val type = monitor.getNetworkType()  // WIFI, CELLULAR, ETHERNET 等

// 观察网络状态变化
lifecycleScope.launch {
    monitor.observeNetworkState().collect { state ->
        when (state) {
            NetworkState.CONNECTED -> { /* 网络已连接 */ }
            NetworkState.DISCONNECTED -> { /* 网络已断开 */ }
        }
    }
}
```

### ViewModel 集成

```kotlin
class UserViewModel : ViewModel() {
    private val ktorKit = KtorKit.getInstance()
    
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Idle)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()
    
    fun loadUser(userId: Int) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            
            ktorKit.get<User>("/users/$userId")
                .onSuccess { user ->
                    _uiState.value = UserUiState.Success(user)
                }
                .onError { code, message, _ ->
                    _uiState.value = UserUiState.Error(message)
                }
        }
    }
    
    sealed class UserUiState {
        data object Idle : UserUiState()
        data object Loading : UserUiState()
        data class Success(val user: User) : UserUiState()
        data class Error(val message: String) : UserUiState()
    }
}
```

## 配置参考

### KtorKitConfig

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `baseUrl` | String | "" | 基础 URL |
| `debug` | Boolean | false | 是否开启调试模式 |
| `requestTimeout` | Long | 30000 | 请求超时（毫秒） |
| `connectTimeout` | Long | 10000 | 连接超时（毫秒） |
| `socketTimeout` | Long | 30000 | Socket 超时（毫秒） |
| `diskCacheSize` | Long | 50MB | 磁盘缓存大小 |
| `defaultCacheTtl` | Long | 5分钟 | 默认缓存过期时间 |
| `enableLogging` | Boolean | true | 是否启用日志 |
| `logLevel` | LogLevel | HEADERS | 日志级别 |
| `userAgent` | String | "KtorKit/1.0" | User-Agent |

### CacheConfig

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `strategy` | CacheStrategy | NETWORK_FIRST | 缓存策略 |
| `ttl` | Long | 5分钟 | 缓存过期时间 |
| `key` | String? | null | 自定义缓存 key |
| `enabled` | Boolean | true | 是否启用缓存 |

### LogLevel

| 级别 | 说明 |
|------|------|
| `NONE` | 不记录日志 |
| `BASIC` | 仅记录方法和 URL |
| `HEADERS` | 记录方法、URL 和请求头 |
| `BODY` | 记录所有信息包括请求体 |

## 项目结构

```
ktorkit/src/main/java/io/kernel/ktor/
├── KtorKit.kt                      # 主入口
├── auth/                           # 认证模块
│   ├── AuthProvider.kt             # 认证提供者接口
│   ├── TokenStorage.kt             # Token 存储接口
│   ├── SharedPrefsTokenStorage.kt  # SharedPreferences 实现
│   └── impl/
│       ├── BearerAuthProvider.kt   # Bearer Token 认证
│       ├── ApiKeyAuthProvider.kt   # API Key 认证
│       └── OAuth2AuthProvider.kt   # OAuth2 认证
├── cache/                          # 缓存模块
│   ├── CacheConfig.kt              # 缓存配置
│   ├── CacheHandler.kt             # 缓存处理器
│   ├── CacheStrategy.kt            # 缓存策略
│   └── DiskCacheManager.kt         # 磁盘缓存管理
├── client/                         # HTTP 客户端
│   ├── HttpClientConfig.kt         # 客户端配置
│   └── HttpClientFactory.kt        # 客户端工厂
├── exception/                      # 异常处理
│   ├── ExceptionHandler.kt         # 异常处理器
│   └── KtorKitException.kt         # 异常定义
├── interceptor/                    # 拦截器
│   ├── AuthInterceptor.kt          # 认证拦截器
│   ├── Interceptor.kt              # 拦截器接口
│   ├── LoggingInterceptor.kt       # 日志拦截器
│   └── RetryInterceptor.kt         # 重试拦截器
├── request/                        # 请求构建
│   └── RequestBuilder.kt           # 请求构建器
├── response/                       # 响应处理
│   ├── ApiResponse.kt              # 响应密封类
│   ├── ResponseMapper.kt           # 响应映射器
│   └── WrappedResponse.kt          # 包装响应模型
└── util/                           # 工具类
    ├── Extensions.kt               # 扩展函数
    └── NetworkMonitor.kt           # 网络监测
```

## 环境要求

- Android API 24+
- Kotlin 2.0+
- Coroutines 1.8+

## 许可证

```
MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
