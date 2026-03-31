# KtorKit

A production-ready Ktor Client wrapper for Android, providing a clean, type-safe API for network requests with built-in support for caching, authentication, interceptors, and error handling.

[![](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![](https://img.shields.io/badge/Kotlin-2.0%2B-blue.svg)](https://kotlinlang.org/)
[![](https://img.shields.io/badge/Ktor-2.3.12-orange.svg)](https://ktor.io/)

## Features

- 🚀 **Simple API** - Clean, intuitive DSL for network requests
- 💾 **Disk Caching** - Built-in disk cache with multiple strategies
- 🔐 **Authentication** - Support for Bearer Token, API Key, OAuth2
- 🔄 **Auto Retry** - Configurable retry with exponential backoff
- 📝 **Logging** - Request/response logging with multiple levels
- 🛡️ **Type Safety** - Full Kotlin serialization support
- ⚡ **Coroutine** - Native coroutine support
- 🎯 **Interceptor Chain** - Request and response interceptors
- ❗ **Error Handling** - Sealed class based error handling

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                    │
├─────────────────────────────────────────────────────────┤
│                      KtorKit API                        │
├─────────────────────────────────────────────────────────┤
│                  RequestBuilder / CacheHandler           │
├─────────────────────────────────────────────────────────┤
│                   Interceptor Chain                     │
│         (Auth → Logging → Retry → Cache)                │
├─────────────────────────────────────────────────────────┤
│                   HttpClient Core                       │
├─────────────────────────────────────────────────────────┤
│                     OkHttp Engine                       │
└─────────────────────────────────────────────────────────┘
```

## Installation

### Step 1: Add the module

Copy the `ktorkit` module to your project, then add to `settings.gradle`:

```groovy
include ':ktorkit'
```

### Step 2: Add dependency

In your app's `build.gradle`:

```groovy
dependencies {
    implementation project(':ktorkit')
}
```

## Quick Start

### 1. Initialize in Application

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        KtorKit.init(this) {
            baseUrl = "https://api.example.com"
            debug = BuildConfig.DEBUG
            
            // Timeout configuration
            requestTimeout = 30_000L
            connectTimeout = 10_000L
            
            // Cache configuration
            diskCacheSize = 50 * 1024 * 1024L  // 50MB
            defaultCacheTtl = 5 * 60 * 1000L   // 5 minutes
            
            // Default headers
            header("X-App-Version", BuildConfig.VERSION_NAME)
            header("Accept-Language", "zh-CN")
        }
    }
}
```

### 2. Make Requests

```kotlin
// Simple GET request
val response = KtorKit.getInstance().get<User>("/users/1")

// POST request with body
val response = KtorKit.getInstance().post<User>(
    path = "/users",
    body = CreateUserRequest("John", "john@example.com")
)

// Handle response
response
    .onSuccess { user -> 
        // Handle success
    }
    .onError { code, message, exception ->
        // Handle error
    }
```

## Usage

### Response Format

KtorKit supports two response formats:

#### Wrapped Response (Recommended)

```kotlin
// Server returns: { "code": 0, "message": "success", "data": {...} }
val response = ktorKit.get<User>("/users/1", wrapped = true)
```

#### Direct Response

```kotlin
// Server returns: { "id": 1, "name": "John" }
val response = ktorKit.get<User>("/users/1", wrapped = false)
```

### Request Builder

For complex requests, use the RequestBuilder:

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

### Caching

KtorKit provides disk caching with multiple strategies:

```kotlin
// Cache strategies
CacheStrategy.NO_CACHE          // No caching
CacheStrategy.CACHE_ONLY        // Only use cache, error if miss
CacheStrategy.NETWORK_ONLY      // Only use network
CacheStrategy.CACHE_FIRST       // Return cache first, then refresh
CacheStrategy.NETWORK_FIRST     // Try network first, fallback to cache
CacheStrategy.CACHE_UNTIL_EXPIRED  // Use cache until expired

// Usage
val response = ktorKit.get<User>(
    path = "/users/1",
    cacheConfig = CacheConfig(
        strategy = CacheStrategy.CACHE_FIRST,
        ttl = CacheConfig.TTL_10_MINUTES
    )
)

// Clear cache
ktorKit.clearCache()
ktorKit.clearExpiredCache()
```

### Authentication

#### Bearer Token

```kotlin
val tokenStorage = SharedPrefsTokenStorage(context)

KtorKit.init(this) {
    authProvider = BearerAuthProvider(
        tokenStorage = tokenStorage,
        refreshCallback = object : TokenRefreshCallback {
            override suspend fun refreshToken(refreshToken: String?): TokenData? {
                // Call refresh token API
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

### Dynamic Base URL

During development, you may need to switch between different environments (dev/test/production). KtorKit provides `updateBaseUrl` method:

```kotlin
// Update baseUrl at runtime
KtorKit.getInstance().updateBaseUrl("https://test-api.example.com")
```

#### Implement Environment Switching in Your App

```kotlin
// Define your environment configuration
data class ApiEnvironment(
    val name: String,
    val baseUrl: String
)

// Manage environments in your app
object AppEnvironment {
    val DEV = ApiEnvironment("Development", "https://dev-api.example.com")
    val TEST = ApiEnvironment("Testing", "https://test-api.example.com")
    val PROD = ApiEnvironment("Production", "https://api.example.com")
    
    fun getAll() = listOf(DEV, TEST, PROD)
    
    fun switch(env: ApiEnvironment) {
        KtorKit.getInstance().updateBaseUrl(env.baseUrl)
    }
}

// Use in settings page
fun showEnvironmentDialog(context: Context) {
    val environments = AppEnvironment.getAll()
    val names = environments.map { "${it.name}: ${it.baseUrl}" }.toTypedArray()
    
    AlertDialog.Builder(context)
        .setTitle("Switch Environment")
        .setItems(names) { _, which ->
            AppEnvironment.switch(environments[which])
        }
        .show()
}
```

### Interceptors

#### Logging Interceptor

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

#### Auth Interceptor

```kotlin
KtorKit.init(this) {
    addRequestInterceptor(AuthInterceptor.create(
        authProvider = bearerAuthProvider,
        "/auth/login",      // Exclude login path
        "/auth/register"    // Exclude register path
    ))
}
```

#### Custom Interceptor

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

// Usage
KtorKit.init(this) {
    addRequestInterceptor(HeaderInterceptor(mapOf(
        "X-Custom-Header" to "value"
    )))
}
```

### Error Handling

```kotlin
val response = ktorKit.get<User>("/users/1")

when (response) {
    is ApiResponse.Success -> {
        val user = response.data
        // Handle success
    }
    is ApiResponse.Error -> {
        val code = response.code
        val message = response.message
        val exception = response.exception
        // Handle error
    }
    is ApiResponse.Loading -> {
        // Handle loading state
    }
}

// Or use extension functions
response
    .onSuccess { user -> /* Handle success */ }
    .onError { code, message, exception -> /* Handle error */ }
```

#### Custom Exception Handler

```kotlin
KtorKit.init(this) {
    exceptionHandler = buildExceptionHandler {
        handle(java.net.SocketTimeoutException::class.java) { e ->
            ApiResponse.Error(code = -999, message = "Request timeout, please retry")
        }
        handle(java.net.UnknownHostException::class.java) { e ->
            ApiResponse.Error(code = -998, message = "Network unavailable")
        }
    }
}
```

### Network Monitoring

```kotlin
val monitor = NetworkMonitor.getInstance(context)

// Check network availability
if (monitor.isNetworkAvailable()) {
    // Network available
}

// Get network type
val type = monitor.getNetworkType()  // WIFI, CELLULAR, ETHERNET, etc.

// Observe network state
lifecycleScope.launch {
    monitor.observeNetworkState().collect { state ->
        when (state) {
            NetworkState.CONNECTED -> { /* Network connected */ }
            NetworkState.DISCONNECTED -> { /* Network disconnected */ }
        }
    }
}
```

### ViewModel Integration

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

## Configuration Reference

### KtorKitConfig

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `baseUrl` | String | "" | Base URL for all requests |
| `debug` | Boolean | false | Enable debug mode |
| `requestTimeout` | Long | 30000 | Request timeout in ms |
| `connectTimeout` | Long | 10000 | Connection timeout in ms |
| `socketTimeout` | Long | 30000 | Socket timeout in ms |
| `diskCacheSize` | Long | 50MB | Maximum disk cache size |
| `defaultCacheTtl` | Long | 5min | Default cache TTL |
| `enableLogging` | Boolean | true | Enable logging |
| `logLevel` | LogLevel | HEADERS | Logging level |
| `userAgent` | String | "KtorKit/1.0" | User-Agent header |

### CacheConfig

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `strategy` | CacheStrategy | NETWORK_FIRST | Cache strategy |
| `ttl` | Long | 5min | Cache TTL |
| `key` | String? | null | Custom cache key |
| `enabled` | Boolean | true | Enable caching |

### LogLevel

| Level | Description |
|-------|-------------|
| `NONE` | No logging |
| `BASIC` | Log method and URL only |
| `HEADERS` | Log method, URL, and headers |
| `BODY` | Log everything including body |

## Project Structure

```
ktorkit/src/main/java/io/kernel/ktor/
├── KtorKit.kt                      # Main entry point
├── auth/                           # Authentication
│   ├── AuthProvider.kt             # Auth provider interface
│   ├── TokenStorage.kt             # Token storage interface
│   ├── SharedPrefsTokenStorage.kt  # SharedPreferences impl
│   └── impl/
│       ├── BearerAuthProvider.kt   # Bearer token auth
│       ├── ApiKeyAuthProvider.kt   # API key auth
│       └── OAuth2AuthProvider.kt   # OAuth2 auth
├── cache/                          # Caching
│   ├── CacheConfig.kt              # Cache configuration
│   ├── CacheHandler.kt             # Cache handler
│   ├── CacheStrategy.kt            # Cache strategies
│   └── DiskCacheManager.kt         # Disk cache manager
├── client/                         # HTTP client
│   ├── HttpClientConfig.kt         # Client configuration
│   └── HttpClientFactory.kt        # Client factory
├── exception/                      # Exception handling
│   ├── ExceptionHandler.kt         # Exception handler
│   └── KtorKitException.kt         # Exception definitions
├── interceptor/                    # Interceptors
│   ├── AuthInterceptor.kt          # Auth interceptor
│   ├── Interceptor.kt              # Interceptor interfaces
│   ├── LoggingInterceptor.kt       # Logging interceptor
│   └── RetryInterceptor.kt         # Retry interceptor
├── request/                        # Request building
│   └── RequestBuilder.kt           # Request builder
├── response/                       # Response handling
│   ├── ApiResponse.kt              # Response sealed class
│   ├── ResponseMapper.kt           # Response mapper
│   └── WrappedResponse.kt          # Wrapped response model
└── util/                           # Utilities
    ├── Extensions.kt               # Extension functions
    └── NetworkMonitor.kt           # Network monitoring
```

## Requirements

- Android API 24+
- Kotlin 2.0+
- Coroutines 1.8+

## License

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
