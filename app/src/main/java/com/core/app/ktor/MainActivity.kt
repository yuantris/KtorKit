package com.core.app.ktor

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.core.app.ktor.data.ArticleViewModel
import com.core.app.ktor.data.UserViewModel
import com.core.app.ktor.ui.theme.KtorKitTheme
import io.kernel.ktor.KtorKit
import io.kernel.ktor.cache.CacheConfig
import io.kernel.ktor.cache.CacheStrategy
import io.kernel.ktor.response.onError
import io.kernel.ktor.response.onSuccess
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val userViewModel by lazy { ViewModelProvider(this)[UserViewModel::class.java] }
    private val articleViewModel by lazy { ViewModelProvider(this)[ArticleViewModel::class.java] }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. 启用Edge-to-Edge，同时关闭导航栏对比度强制（去掉遮罩）
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
        // 2. 强制关闭导航栏对比度遮罩（Android16必加）
        window.isNavigationBarContrastEnforced = false
        setContent {
            KtorKitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KtorKitDemoScreen(
                        modifier = Modifier.padding(innerPadding),
                        userViewModel = userViewModel,
                        articleViewModel = articleViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun KtorKitDemoScreen(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel,
    articleViewModel: ArticleViewModel
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var resultText by remember { mutableStateOf("点击按钮开始请求...") }
    val userUiState by userViewModel.uiState.collectAsState()
    val articleUiState by articleViewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "KtorKit Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 示例 1: 使用便捷方法发送 GET 请求
        Button(
            onClick = {
                coroutineScope.launch {
                    resultText = "请求中..."

                    // 使用 KtorKit 单例发送请求
                    val ktorKit = KtorKit.getInstance()

                    // 包装格式响应
                    val response = ktorKit.get<String>(
                        path = "/api/status",
                        wrapped = true,
                        cacheConfig = CacheConfig(
                            strategy = CacheStrategy.NETWORK_FIRST,
                            ttl = CacheConfig.DEFAULT_TTL
                        )
                    )

                    response
                        .onSuccess { data ->
                            resultText = "成功: $data"
                        }
                        .onError { code, message, exception ->
                            resultText = "失败 [$code]: $message"
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("发送 GET 请求 (包装格式)")
        }

        // 示例 2: 直接格式响应
        Button(
            onClick = {
                coroutineScope.launch {
                    resultText = "请求中..."

                    val ktorKit = KtorKit.getInstance()
                    val response = ktorKit.get<String>(
                        path = "/api/data",
                        wrapped = false // 直接响应格式
                    )

                    response
                        .onSuccess { data ->
                            resultText = "成功: $data"
                        }
                        .onError { code, message, exception ->
                            resultText = "失败 [$code]: $message"
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("发送 GET 请求 (直接格式)")
        }

        // 示例 3: 使用 RequestBuilder 构建复杂请求
        Button(
            onClick = {
                coroutineScope.launch {
                    resultText = "请求中..."

                    val ktorKit = KtorKit.getInstance()
                    val response = ktorKit.request()
                        .path("/api/users")
                        .get()
                        .query("page", 1)
                        .query("pageSize", 10)
                        .header("X-Custom-Header", "value")
                        .cache(CacheConfig(
                            strategy = CacheStrategy.CACHE_FIRST,
                            ttl = CacheConfig.TTL_10_MINUTES
                        ))
                        .executeString()

                    response
                        .onSuccess { data ->
                            resultText = "成功: $data"
                        }
                        .onError { code, message, exception ->
                            resultText = "失败 [$code]: $message"
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("使用 RequestBuilder")
        }

        // 示例 4: POST 请求
        Button(
            onClick = {
                coroutineScope.launch {
                    resultText = "请求中..."

                    val ktorKit = KtorKit.getInstance()
                    val response = ktorKit.post<String>(
                        path = "/api/users",
                        body = mapOf("name" to "张三", "email" to "test@example.com"),
                        wrapped = true
                    )

                    response
                        .onSuccess { data ->
                            resultText = "成功: $data"
                        }
                        .onError { code, message, exception ->
                            resultText = "失败 [$code]: $message"
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("发送 POST 请求")
        }

        // 示例 5: 使用 ViewModel
        Button(
            onClick = {
                userViewModel.loadUsers()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("使用 ViewModel 加载用户")
        }

        // 显示用户状态
        when (val state = userUiState) {
            is UserViewModel.UserUiState.Loading -> {
                CircularProgressIndicator()
            }
            is UserViewModel.UserUiState.UsersLoaded -> {
                Text(
                    text = "加载成功: ${state.users.size} 个用户",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            is UserViewModel.UserUiState.Error -> {
                Text(
                    text = "错误: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 显示结果
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "请求结果:",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 使用说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "使用说明",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        1. 在 Application 中初始化 KtorKit
                        2. 使用 KtorKit.getInstance() 获取实例
                        3. 使用便捷方法 (get/post/put/delete) 或 RequestBuilder
                        4. 支持包装格式和直接格式响应
                        5. 支持缓存、认证、重试等功能
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
