package com.example.mallproject.repository

import com.example.mallproject.model.Product
import com.example.mallproject.network.ProductApi

class ProductRepository(
    private val api: ProductApi,
    private val fallback: List<Product> = emptyList()
) {
    suspend fun getProducts(): List<Product> =
        runCatching { api.fetchProducts() }.onFailure { }.getOrDefault(fallback)
}