package com.example.sms



data class Message(
    val id: String? = "",
    val senderId: String? = "",
    val receiverId: String? = "",
    val text: String? = "",
    val timestamp: Long = System.currentTimeMillis()
)
