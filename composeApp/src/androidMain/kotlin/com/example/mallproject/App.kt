package com.example.mallproject

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.example.mallproject.model.CartItem
import com.example.mallproject.model.Product
import com.example.mallproject.model.ShopOrder
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


enum class Tab { Home, Cart, Orders }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val vm: ShopViewModel = koinViewModel()
        val ui = vm.uiState

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when {
                                ui.inDetail -> "商品详情"
                                ui.currentTab == Tab.Home -> "KMP Mini Shopping"
                                ui.currentTab == Tab.Cart -> "购物车"
                                else -> "订单列表"
                            }
                        )
                    },
                    actions = {
                        if (ui.inDetail) {
                            TextButton(onClick = { vm.closeDetail() }) { Text("返回") }
                        } else {
                            Text(
                                text = "共 ${ui.totalCount} 件",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!ui.inDetail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = ui.currentTab == Tab.Home,
                            onClick = { vm.selectTab(Tab.Home) },
                            icon = { Text("🏠") },
                            label = { Text("商品") }
                        )
                        NavigationBarItem(
                            selected = ui.currentTab == Tab.Cart,
                            onClick = { vm.selectTab(Tab.Cart) },
                            icon = { Text("🛒") },
                            label = { Text("购物车(${ui.totalCount})") }
                        )
                        NavigationBarItem(
                            selected = ui.currentTab == Tab.Orders,
                            onClick = { vm.selectTab(Tab.Orders) },
                            icon = { Text("📦") },
                            label = { Text("订单") }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                when {
                    ui.inDetail -> ProductDetailScreen(
                        product = vm.selectedProduct,
                        quantity = vm.selectedProduct?.let { vm.quantityOf(it.id) } ?: 0,
                        onAdd = {
                            val msg = vm.addToCart(it)
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    )

                    ui.currentTab == Tab.Home -> ProductListScreen(
                        state = ui.productListState,
                        products = vm.filteredProducts,
                        totalProducts = ui.products.size,
                        query = ui.query,
                        quantityOf = vm::quantityOf,
                        onQueryChange = vm::onQueryChange,
                        onRetry = { vm.dispatch(ProductListIntent.Retry) },
                        onAdd = {
                            val msg = vm.addToCart(it)
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        onClick = vm::openDetail
                    )

                    ui.currentTab == Tab.Cart -> CartScreen(
                        items = ui.cartItems,
                        totalPrice = ui.totalPrice,
                        onInc = vm::increase,
                        onDec = vm::decrease,
                        onRemove = vm::remove,
                        onClear = vm::clear,
                        onCheckout = {
                            val msg = vm.checkout()
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    )

                    else -> OrderScreen(ui.orders)
                }
            }
        }
    }
}

@Composable
private fun ProductListScreen(
    state: ProductListState,
    products: List<Product>,
    totalProducts: Int,
    query: String,
    quantityOf: (String) -> Int,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onAdd: (Product) -> Unit,
    onClick: (Product) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            label = { Text("搜索课程") },
            placeholder = { Text("输入标题或描述") }
        )

        Text(
            text = "共 $totalProducts 条 · 当前显示 ${products.size} 条",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when (state) {
            ProductListState.Loading -> EmptyState("⏳ 加载中...")

            is ProductListState.Error -> {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("加载失败：${state.message}", color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = onRetry) { Text("重试") }
                    }
                }
            }

            ProductListState.Empty -> EmptyState("没有匹配的商品")

            is ProductListState.Success -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    verticalItemSpacing = 10.dp,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    staggeredItems(products, key = { it.id }) { p ->
                        ElevatedCard(onClick = { onClick(p) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProductCover(p.imageUrl)
                                Text(
                                    p.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                PriceTag(p.price)
                                Text(
                                    p.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { onAdd(p) }) { Text("加购") }
                                    Spacer(Modifier.width(8.dp))
                                    val q = quantityOf(p.id)
                                    if (q > 0) {
                                        Text(
                                            text = "x$q",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDetailScreen(
    product: Product?,
    quantity: Int,
    onAdd: (Product) -> Unit
) {
    if (product == null) {
        EmptyState("商品不存在")
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProductCover(product.imageUrl)
                Text(product.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                PriceTag(product.price)
                Text(product.description)
                if (quantity > 0) {
                    Text("当前已加购：$quantity", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Button(onClick = { onAdd(product) }, modifier = Modifier.fillMaxWidth()) {
            Text("加入购物车")
        }
    }
}

@Composable
private fun CartScreen(
    items: List<CartItem>,
    totalPrice: Double,
    onInc: (String) -> Unit,
    onDec: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (items.isEmpty()) {
            EmptyState("购物车为空")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(items) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.title, fontWeight = FontWeight.Medium)
                                Text("¥${formatPrice(item.price * item.quantity)}")
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("¥${formatPrice(item.price)} x ${item.quantity} = ¥${formatPrice(item.price * item.quantity)}")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SmallOpButton("-") { onDec(item.productId) }
                                Text("  ${item.quantity}  ")
                                SmallOpButton("+") { onInc(item.productId) }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { onRemove(item.productId) }) { Text("移除") }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("总件数：${items.sumOf { it.quantity }}")
                        Text("合计：¥${formatPrice(totalPrice)}", fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onClear) { Text("清空") }
                        Button(onClick = onCheckout) { Text("结算") }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderScreen(orders: List<ShopOrder>) {
    if (orders.isEmpty()) {
        EmptyState("还没有订单")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(orders) { order ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("订单号：${order.orderId}", fontWeight = FontWeight.Bold)
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    Text("下单时间：${sdf.format(Date(order.createdAt))}")
                    Text("状态：${order.status}")
                    Text("总价：¥${formatPrice(order.totalPrice)}")
                    Text("商品数：${order.items.sumOf { it.quantity }}")
                    order.items.forEach { item ->
                        Text("- ${item.title} x${item.quantity}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCover(url: String) {
    val coverModifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
        .clip(RoundedCornerShape(12.dp))

    if (url.isNotBlank()) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            modifier = coverModifier,
            contentScale = ContentScale.Crop,
            loading = {
                Image(
                    painter = painterResource(id = R.drawable.course_cover_default),
                    contentDescription = null,
                    modifier = coverModifier,
                    contentScale = ContentScale.Crop
                )
            },
            error = {
                Image(
                    painter = painterResource(id = R.drawable.course_cover_default),
                    contentDescription = null,
                    modifier = coverModifier,
                    contentScale = ContentScale.Crop
                )
            },
            success = { SubcomposeAsyncImageContent() }
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.course_cover_default),
            contentDescription = null,
            modifier = coverModifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun SmallOpButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Text(text)
    }
}

@Composable
private fun PriceTag(price: Double) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = "¥${formatPrice(price)}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧺", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun formatPrice(price: Double): String = "%.2f".format(price)
