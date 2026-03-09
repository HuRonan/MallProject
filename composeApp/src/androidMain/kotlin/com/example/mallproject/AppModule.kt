package com.example.mallproject

import com.example.mallproject.network.ProductApi
import com.example.mallproject.repository.ProductRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    single { ProductApi(baseUrl = "https://shopneu.hmos.site") }
    single { ProductRepository(api = get()) }
    viewModel { ShopViewModel(productRepository = get()) }
}

fun initAppKoin() {
    if (GlobalContext.getOrNull() != null) return
    startKoin {
        modules(appModule)
    }
}