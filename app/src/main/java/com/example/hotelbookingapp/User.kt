package com.example.hotelbookingapp

data class User(
    val fullName:  String = "",
    val email:     String = "",
    val role:      String = UserRole.GUEST.name,
    val createdAt: Long   = System.currentTimeMillis(),
    val fcmToken:  String = "",
    val points:    Int    = 0
)

enum class UserRole { GUEST, HOST }