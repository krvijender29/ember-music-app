package com.example.ember.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val token: String? = null,
    val isGuest: Boolean = false
)
