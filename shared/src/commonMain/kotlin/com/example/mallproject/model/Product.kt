package com.example.mallproject.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val title: String,
    val price: Double,
    val description: String,
    val imageUrl: String = ""
)