package com.myorderapp.ui.cart

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myorderapp.domain.model.CartState
import com.myorderapp.ui.components.CozyCard
import com.myorderapp.ui.components.CozyCocoa
import com.myorderapp.ui.components.CozyMuted
import com.myorderapp.ui.components.CozyPrimaryButton
import com.myorderapp.ui.components.CozyRose
import com.myorderapp.ui.util.yuanText
import org.koin.androidx.compose.koinViewModel

@Composable
fun CartScreen(
    viewModel: CartViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onCheckoutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cart = uiState.cartState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFEF8F2))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CartTopBar(
                cart = cart,
                onBack = onBack,
                onClear = viewModel::clear
            )

            Box(modifier = Modifier.weight(1f)) {
                if (cart.isEmpty) {
                    EmptyCartState(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cart.items, key = { it.id }) { item ->
                            CozyCard(
                                containerColor = Color.White.copy(alpha = 0.62f),
                                borderColor = Color.White.copy(alpha = 0.72f),
                                radius = 24,
                                contentPadding = PaddingValues(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(item.menuItemName, color = CozyCocoa, fontWeight = FontWeight.Black)
                                        Text(yuanText(item.unitPrice), color = CozyRose, fontWeight = FontWeight.SemiBold)
                                    }
                                    QuantityStepper(
                                        quantity = item.quantity,
                                        onDecrease = { viewModel.decrease(item.menuItemId) },
                                        onIncrease = { viewModel.increase(item.menuItemId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CartSummaryBar(
                cart = cart,
                onCheckoutClick = onCheckoutClick
            )
        }
    }
}

@Composable
private fun CartTopBar(
    cart: CartState,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = CozyRose)
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("购物篮", color = CozyRose, fontWeight = FontWeight.Black)
            Text(
                text = if (cart.isEmpty) "还没有选择菜品" else "共 ${cart.itemCount} 份菜品",
                color = CozyMuted
            )
        }
        if (!cart.isEmpty) {
            TextButton(
                onClick = onClear,
                colors = ButtonDefaults.textButtonColors(contentColor = CozyRose),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("清空")
            }
        }
    }
}

@Composable
private fun EmptyCartState(modifier: Modifier = Modifier) {
    CozyCard(
        modifier = modifier.padding(28.dp),
        containerColor = Color.White.copy(alpha = 0.62f),
        borderColor = Color.White.copy(alpha = 0.72f),
        radius = 24
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = Color(0xFFFFD1DC).copy(alpha = 0.62f)) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = CozyRose,
                    modifier = Modifier.padding(18.dp).size(36.dp)
                )
            }
            Text("购物篮为空", color = CozyCocoa, fontWeight = FontWeight.Black)
            Text("回到点菜页添加喜欢的菜品。", color = CozyMuted)
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StepperButton(icon = Icons.Default.Remove, contentDescription = "减少", onClick = onDecrease)
        Text(quantity.toString(), color = CozyCocoa, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp))
        StepperButton(icon = Icons.Default.Add, contentDescription = "增加", onClick = onIncrease)
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFFFFD1DC).copy(alpha = 0.72f)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = CozyRose, modifier = Modifier.padding(8.dp).size(18.dp))
    }
}

@Composable
private fun CartSummaryBar(
    cart: CartState,
    onCheckoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF8F2).copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        CozyCard(
            containerColor = Color.White.copy(alpha = 0.66f),
            borderColor = Color.White.copy(alpha = 0.72f),
            radius = 30
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("小计", color = CozyMuted)
                    Text(yuanText(cart.subtotal), color = CozyCocoa, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("合计", color = CozyCocoa, fontWeight = FontWeight.Black)
                    Text(yuanText(cart.totalPrice), color = CozyRose, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(2.dp))
                CozyPrimaryButton(
                    text = "去结算",
                    onClick = onCheckoutClick,
                    enabled = !cart.isEmpty,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
