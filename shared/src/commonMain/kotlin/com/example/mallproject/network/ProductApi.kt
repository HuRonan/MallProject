package com.example.mallproject.network

import com.example.mallproject.model.Product
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class ProductApi(
    private val baseUrl: String,
    private val client: HttpClient = createHttpClient()
) {
    suspend fun fetchProducts(): List<Product> {
        return client.get("$baseUrl/products").body()
    }
}