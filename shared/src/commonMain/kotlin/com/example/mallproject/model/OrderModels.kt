package com.example.mallproject.model

data class OrderItem(
    val productId: String,
    val title: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int
)

data class ShopOrder(
    val orderId: String,
    val totalPrice: Double,
    val status: String,
    val createdAt: Long,
    val items: List<OrderItem>
)