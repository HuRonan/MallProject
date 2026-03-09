package com.example.mallproject.repository

import com.example.mallproject.db.ShopDatabase
import com.example.mallproject.model.CartItem
import com.example.mallproject.model.OrderItem
import com.example.mallproject.model.ShopOrder
import com.example.mallproject.util.currentTimeMillis
import kotlin.random.Random


class OrderRepository(
    private val database: ShopDatabase
) {
    private val queries = database.ordersQueries

    fun createOrder(cartItems: List<CartItem>): ShopOrder? {
        if (cartItems.isEmpty()) return null

        val orderId = buildOrderId()
        val createdAt = currentTimeMillis()
        val totalPrice = cartItems.sumOf { it.price * it.quantity }
        val items = cartItems.map {
            OrderItem(
                productId = it.productId,
                title = it.title,
                price = it.price,
                imageUrl = it.imageUrl,
                quantity = it.quantity
            )
        }

        queries.insertOrder(
            order_id = orderId,
            total_price = totalPrice,
            status = "CREATED",
            created_at = createdAt
        )

        items.forEach { item ->
            queries.insertOrderItem(
                order_id = orderId,
                product_id = item.productId,
                title = item.title,
                price = item.price,
                image_url = item.imageUrl,
                quantity = item.quantity.toLong()
            )
        }

        return ShopOrder(
            orderId = orderId,
            totalPrice = totalPrice,
            status = "CREATED",
            createdAt = createdAt,
            items = items
        )
    }

    fun getOrders(): List<ShopOrder> {
        return queries.selectOrders().executeAsList().map { order ->
            val items = queries.selectItemsByOrderId(order.order_id).executeAsList().map { item ->
                OrderItem(
                    productId = item.product_id,
                    title = item.title,
                    price = item.price,
                    imageUrl = item.image_url,
                    quantity = item.quantity.toInt()
                )
            }
            ShopOrder(
                orderId = order.order_id,
                totalPrice = order.total_price,
                status = order.status,
                createdAt = order.created_at,
                items = items
            )
        }
    }

    private fun buildOrderId(): String = "ORD-${currentTimeMillis()}-${Random.nextInt(100, 999)}"
}