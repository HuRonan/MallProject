package com.example.mallproject.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(ShopDatabase.Schema, appContext, "shop.db")
    }
}