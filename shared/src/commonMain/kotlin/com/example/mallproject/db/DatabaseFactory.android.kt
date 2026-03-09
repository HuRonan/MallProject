package com.example.mallproject.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseFactory() {
    fun createDriver(): SqlDriver
}

fun createShopDatabase(factory: DatabaseFactory = DatabaseFactory()): ShopDatabase {
    return ShopDatabase(factory.createDriver())
}