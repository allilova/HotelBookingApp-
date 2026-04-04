package com.example.hotelbookingapp

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole { GUEST, HOST }

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val email: String,
    val passwordHash: String,   // SHA-256 hex
    val role: String = UserRole.GUEST.name,   // "GUEST" or "HOST"
    val createdAt: Long = System.currentTimeMillis()
)