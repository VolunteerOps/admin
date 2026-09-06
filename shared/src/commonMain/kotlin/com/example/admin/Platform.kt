package com.example.admin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform