package com.example.mallproject.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
    val productId: String,
    val quantity: Int,
    val title: String,
    val price: Double,
    val imageUrl: String
){}