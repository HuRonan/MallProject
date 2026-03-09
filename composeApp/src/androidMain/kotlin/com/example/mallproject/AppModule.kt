package com.example.mallproject

import com.example.mallproject.db.createShopDatabase
import com.example.mallproject.network.ProductApi
import com.example.mallproject.repository.CartRepository
import com.example.mallproject.repository.OrderRepository
import com.example.mallproject.repository.ProductRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    single { createShopDatabase() }
    single { ProductApi(baseUrl = "https://shopneu.hmos.site") }
    single { ProductRepository(api = get()) }
    single { CartRepository(database = get()) }
    single { OrderRepository(database = get()) }
    viewModel { ShopViewModel(productRepository = get(), cartRepository = get(), orderRepository = get()) }
}

fun initAppKoin() {
    if (GlobalContext.getOrNull() != null) return
    startKoin {
        modules(appModule)
    }
}