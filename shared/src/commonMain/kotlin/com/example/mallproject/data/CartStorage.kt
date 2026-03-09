package com.example.mallproject.data

import com.example.mallproject.model.CartItem
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CartStorage(
    private val settings: Settings = Settings(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val key = "cart_items"

    fun load(): List<CartItem> {
        val raw = settings.getStringOrNull(key) ?: return emptyList()
        return runCatching { json.decodeFromString<List<CartItem>>(raw) }
            .getOrDefault(emptyList())
    }

    fun save(items: List<CartItem>) {
        settings.putString(key, json.encodeToString(items))
    }
}