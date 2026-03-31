package com.core.app.ktor.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.kernel.ktor.KtorKit
import io.kernel.ktor.response.ApiResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 用户 ViewModel 示例
 */
class UserViewModel : ViewModel() {

    private val ktorKit: KtorKit by lazy {
        KtorKit.getInstance()
    }

    private val userRepository by lazy { UserRepository(ktorKit) }

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Idle)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    /**
     * 登录
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading

            val response = userRepository.login(username, password)
            when (response) {
                is ApiResponse.Success -> {
                    _uiState.value = UserUiState.LoginSuccess(response.data)
                }
                is ApiResponse.Error -> {
                    _uiState.value = UserUiState.Error(response.message)
                }
                is ApiResponse.Loading -> {
                    // 保持 Loading 状态
                }
            }
        }
    }

    /**
     * 加载用户信息
     */
    fun loadUser(userId: Int) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading

            val response = userRepository.getUser(userId)
            when (response) {
                is ApiResponse.Success -> {
                    _uiState.value = UserUiState.UserLoaded(response.data)
                }
                is ApiResponse.Error -> {
                    _uiState.value = UserUiState.Error(response.message)
                }
                is ApiResponse.Loading -> {
                    // 保持 Loading 状态
                }
            }
        }
    }

    /**
     * 加载用户列表
     */
    fun loadUsers(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading

            val response = userRepository.getUsers(page)
            when (response) {
                is ApiResponse.Success -> {
                    _uiState.value = UserUiState.UsersLoaded(response.data)
                }
                is ApiResponse.Error -> {
                    _uiState.value = UserUiState.Error(response.message)
                }
                is ApiResponse.Loading -> {
                    // 保持 Loading 状态
                }
            }
        }
    }

    /**
     * UI 状态
     */
    sealed class UserUiState {
        data object Idle : UserUiState()
        data object Loading : UserUiState()
        data class LoginSuccess(val response: LoginResponse) : UserUiState()
        data class UserLoaded(val user: User) : UserUiState()
        data class UsersLoaded(val users: List<User>) : UserUiState()
        data class Error(val message: String) : UserUiState()
    }
}

/**
 * 文章 ViewModel 示例
 */
class ArticleViewModel : ViewModel() {

    private val ktorKit: KtorKit by lazy {
        KtorKit.getInstance()
    }

    private val articleRepository by lazy { ArticleRepository(ktorKit) }

    private val _uiState = MutableStateFlow<ArticleUiState>(ArticleUiState.Idle)
    val uiState: StateFlow<ArticleUiState> = _uiState.asStateFlow()

    /**
     * 加载文章列表
     */
    fun loadArticles(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = ArticleUiState.Loading

            val response = articleRepository.getArticles(page)
            when (response) {
                is ApiResponse.Success -> {
                    _uiState.value = ArticleUiState.ArticlesLoaded(response.data)
                }
                is ApiResponse.Error -> {
                    _uiState.value = ArticleUiState.Error(response.message)
                }
                is ApiResponse.Loading -> {
                    // 保持 Loading 状态
                }
            }
        }
    }

    /**
     * 加载文章详情
     */
    fun loadArticle(articleId: Int) {
        viewModelScope.launch {
            _uiState.value = ArticleUiState.Loading

            val response = articleRepository.getArticle(articleId)
            when (response) {
                is ApiResponse.Success -> {
                    _uiState.value = ArticleUiState.ArticleLoaded(response.data)
                }
                is ApiResponse.Error -> {
                    _uiState.value = ArticleUiState.Error(response.message)
                }
                is ApiResponse.Loading -> {
                    // 保持 Loading 状态
                }
            }
        }
    }

    /**
     * 搜索文章
     */
    fun searchArticles(keyword: String) {
        viewModelScope.launch {
            _uiState.value = ArticleUiState.Loading

            val response = articleRepository.searchArticles(keyword)
            when (response) {
                is ApiResponse.Success -> {
                    _uiState.value = ArticleUiState.ArticlesLoaded(response.data)
                }
                is ApiResponse.Error -> {
                    _uiState.value = ArticleUiState.Error(response.message)
                }
                is ApiResponse.Loading -> {
                    // 保持 Loading 状态
                }
            }
        }
    }

    /**
     * UI 状态
     */
    sealed class ArticleUiState {
        data object Idle : ArticleUiState()
        data object Loading : ArticleUiState()
        data class ArticlesLoaded(val articles: List<Article>) : ArticleUiState()
        data class ArticleLoaded(val article: Article) : ArticleUiState()
        data class Error(val message: String) : ArticleUiState()
    }
}
