package com.example.mallproject.repository

import com.example.mallproject.db.ShopDatabase
import com.example.mallproject.model.CartItem
import com.example.mallproject.model.Product
import com.example.mallproject.util.currentTimeMillis

class CartRepository(
    private val database: ShopDatabase
) {
    private val queries = database.cartQueries

    fun items(): List<CartItem> {
        return queries.selectAll().executeAsList().map {
            CartItem(
                productId = it.product_id,
                quantity = it.quantity.toInt(),
                title = it.title,
                price = it.price,
                imageUrl = it.image_url
            )
        }
    }

    fun add(product: Product) {
        val current = queries.selectById(product.id).executeAsOneOrNull()
        val nextQuantity = (current?.quantity?.toInt() ?: 0) + 1
        upsert(product, nextQuantity)
    }

    fun increase(productId: String) {
        val current = queries.selectById(productId).executeAsOneOrNull() ?: return
        queries.insertOrReplace(
            product_id = current.product_id,
            title = current.title,
            price = current.price,
            image_url = current.image_url,
            quantity = current.quantity + 1,
            updated_at = nowMillis()
        )
    }

    fun decrease(productId: String) {
        val current = queries.selectById(productId).executeAsOneOrNull() ?: return
        val next = current.quantity.toInt() - 1
        if (next <= 0) {
            queries.deleteById(productId)
        } else {
            queries.insertOrReplace(
                product_id = current.product_id,
                title = current.title,
                price = current.price,
                image_url = current.image_url,
                quantity = next.toLong(),
                updated_at = nowMillis()
            )
        }
    }

    fun remove(productId: String) {
        queries.deleteById(productId)
    }

    fun clear() {
        queries.deleteAll()
    }

    private fun upsert(product: Product, quantity: Int) {
        queries.insertOrReplace(
            product_id = product.id,
            title = product.title,
            price = product.price,
            image_url = product.imageUrl,
            quantity = quantity.toLong(),
            updated_at = nowMillis()
        )
    }

    private fun nowMillis(): Long = currentTimeMillis()
}