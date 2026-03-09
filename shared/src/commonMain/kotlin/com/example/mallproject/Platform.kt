package com.example.mallproject

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform