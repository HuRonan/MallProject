package com.example.mallproject

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.mallproject.model.CartItem
import com.example.mallproject.model.Product
import com.example.mallproject.model.ShopOrder
import com.example.mallproject.repository.CartRepository
import com.example.mallproject.repository.OrderRepository
import com.example.mallproject.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

sealed interface ProductListIntent {
    data object Load : ProductListIntent
    data object Retry : ProductListIntent
    data class AddToCart(val product: Product) : ProductListIntent
    data class Search(val query: String) : ProductListIntent
}

sealed interface ProductListState {
    data object Loading : ProductListState
    data class Success(val products: List<Product>) : ProductListState
    data class Error(val message: String) : ProductListState
    data object Empty : ProductListState
}

data class ShopUiState(
    val currentTab: Tab = Tab.Home,
    val inDetail: Boolean = false,
    val selectedProductId: String? = null,
    val query: String = "",
    val products: List<Product> = emptyList(),
    val productListState: ProductListState = ProductListState.Loading,
    val cartItems: List<CartItem> = emptyList(),
    val totalCount: Int = 0,
    val totalPrice: Double = 0.0,
    val orders: List<ShopOrder> = emptyList()
)

class ShopViewModel(
    val productRepository: ProductRepository,
    val cartRepository: CartRepository,
    val orderRepository: OrderRepository
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var uiState by mutableStateOf(ShopUiState())
        private set

    val filteredProducts: List<Product>
        get() {
            val normalized = uiState.query.trim()
            return if (normalized.isBlank()) {
                uiState.products
            } else {
                uiState.products.filter {
                    it.title.contains(normalized, ignoreCase = true) ||
                            it.description.contains(normalized, ignoreCase = true)
                }
            }
        }

    val selectedProduct: Product?
        get() = uiState.products.firstOrNull { it.id == uiState.selectedProductId }

    init {
        dispatch(ProductListIntent.Load)
        syncCart()
        syncOrders()
    }

    fun dispatch(intent: ProductListIntent) {
        when (intent) {
            ProductListIntent.Load,
            ProductListIntent.Retry -> refreshProducts()

            is ProductListIntent.Search -> {
                uiState = uiState.copy(query = intent.query)
            }

            is ProductListIntent.AddToCart -> {
                cartRepository.add(intent.product)
                syncCart()
            }
        }
    }

    fun refreshProducts() {
        scope.launch {
            uiState = uiState.copy(productListState = ProductListState.Loading)

            runCatching { productRepository.getProducts() }
                .onSuccess { list ->
                    uiState = uiState.copy(
                        products = list,
                        productListState = if (list.isEmpty()) {
                            ProductListState.Empty
                        } else {
                            ProductListState.Success(list)
                        }
                    )
                    syncCart()
                }
                .onFailure {
                    uiState = uiState.copy(
                        productListState = ProductListState.Error(it.message ?: "加载失败")
                    )
                }
        }
    }

    fun onQueryChange(value: String) {
        dispatch(ProductListIntent.Search(value))
    }

    fun selectTab(tab: Tab) {
        uiState = uiState.copy(currentTab = tab)
    }

    fun openDetail(product: Product) {
        uiState = uiState.copy(inDetail = true, selectedProductId = product.id)
    }

    fun closeDetail() {
        uiState = uiState.copy(inDetail = false)
    }

    fun quantityOf(productId: String): Int =
        uiState.cartItems.firstOrNull { it.productId == productId }?.quantity ?: 0

    fun addToCart(product: Product): String {
        dispatch(ProductListIntent.AddToCart(product))
        return "已加入购物车"
    }

    fun increase(productId: String) {
        cartRepository.increase(productId)
        syncCart()
    }

    fun decrease(productId: String) {
        cartRepository.decrease(productId)
        syncCart()
    }

    fun remove(productId: String) {
        cartRepository.remove(productId)
        syncCart()
    }

    fun clear() {
        cartRepository.clear()
        syncCart()
    }

    fun checkout(): String {
        val cartItems = cartRepository.items()
        if (cartItems.isEmpty()) {
            return "购物车为空"
        }

        val order = orderRepository.createOrder(cartItems)
            ?: return "创建订单失败"

        cartRepository.clear()
        syncCart()
        syncOrders()
        uiState = uiState.copy(currentTab = Tab.Orders)
        return "下单成功，订单号：${order.orderId}"
    }

    private fun syncCart() {
        val items = cartRepository.items()
        uiState = uiState.copy(
            cartItems = items,
            totalCount = items.sumOf { it.quantity },
            totalPrice = items.sumOf { it.price * it.quantity }
        )
    }

    private fun syncOrders() {
        uiState = uiState.copy(orders = orderRepository.getOrders())
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}