package com.core.app.ktor.data

import kotlinx.serialization.Serializable

/**
 * 用户模型
 */
@Serializable
data class User(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val avatar: String? = null
)

/**
 * 登录请求
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * 登录响应
 */
@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: User
)

/**
 * 文章模型
 */
@Serializable
data class Article(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val author: User? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

/**
 * 分页参数
 */
@Serializable
data class PageParams(
    val page: Int = 1,
    val pageSize: Int = 20
)
