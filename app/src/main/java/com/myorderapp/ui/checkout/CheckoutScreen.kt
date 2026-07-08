package com.myorderapp.ui.checkout

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myorderapp.domain.model.CartState
import com.myorderapp.ui.components.CandyCoinIcon
import com.myorderapp.ui.components.CozyCard
import com.myorderapp.ui.components.CozyCocoa
import com.myorderapp.ui.components.CozyMuted
import com.myorderapp.ui.components.CozyPill
import com.myorderapp.ui.components.CozyRose
import com.myorderapp.ui.components.cozyTextFieldColors
import com.myorderapp.ui.util.yuanText
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onOrderSubmitted: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cart = uiState.cartState
    var successOrderId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.orderSubmittedId) {
        uiState.orderSubmittedId?.let { orderId ->
            successOrderId = orderId
            delay(900)
            onOrderSubmitted(orderId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFEF8F2))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(196.dp) 
                .background(Color(0xFFFFDCE6).copy(alpha = 0.30f)) 
        )
        Column(modifier = Modifier.fillMaxSize()) {
            CheckoutTopBar(onBack = onBack)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { CheckoutMoodCard(cart = cart) }
                item { CheckoutItemsCard(cart = cart) }
                item {
                    CheckoutCandyCoinsCard(
                        balance = uiState.candyCoins,
                        cost = candyCoinsCost(cart.totalPrice),
                        isCartEmpty = cart.isEmpty
                    )
                }

                uiState.errorMessage?.let { message ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f)
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    CozyCard(
                        containerColor = Color(0xFFFFFCF8).copy(alpha = 0.62f),
                        borderColor = Color.White.copy(alpha = 0.68f),
                        radius = 28,
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "给后厨的悄悄话",
                                color = CozyCocoa,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "口味、忌口、想说的话都可以写在这里",
                                color = CozyMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = uiState.buyerNote,
                                onValueChange = viewModel::onBuyerNoteChange,
                                placeholder = { Text("比如少辣、多加葱、今天想吃热一点...") },
                                minLines = 3,
                                colors = cozyTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            CheckoutBottomAction(
                cart = cart,
                candyCoins = uiState.candyCoins,
                isSubmitting = uiState.isSubmitting,
                onSubmit = viewModel::submitOrder
            )
        }
        if (successOrderId != null) {
            CheckoutSuccessDialog()
        }
    }
}

@Composable
private fun CheckoutSuccessDialog() {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Color(0xFFFFFCF8),
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CozyRose, modifier = Modifier.size(42.dp))
        },
        title = {
            Text("点菜成功", color = CozyCocoa, fontWeight = FontWeight.Black)
        },
        text = {
            Text("已经告诉对方啦～", color = CozyMuted)
        },
        confirmButton = {}
    )
}

@Composable
private fun CheckoutTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = CozyRose)
        }
        Text(
            text = "确认点菜",
            color = CozyRose,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun CheckoutMoodCard(cart: CartState) {
    CozyCard(
        containerColor = Color.White.copy(alpha = 0.42f),
        borderColor = Color.White.copy(alpha = 0.72f),
        radius = 30,
        contentPadding = PaddingValues(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = CircleShape, color = Color(0xFFFFD1DC).copy(alpha = 0.62f)) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = CozyRose,
                    modifier = Modifier.padding(12.dp).size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (cart.isEmpty) "购物篮还是空的" else "今晚这顿安排好啦",
                    color = CozyCocoa,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (cart.isEmpty) "先去点几道想吃的菜吧" else "预计 20 分钟后享用",
                    color = CozyMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            CozyPill(if (cart.isEmpty) "待点菜" else "待提交", color = CozyRose)
        }
    }
}

@Composable
private fun CheckoutItemsCard(cart: CartState) {
    CozyCard(
        containerColor = Color.White.copy(alpha = 0.62f),
        borderColor = Color.White.copy(alpha = 0.72f),
        radius = 30,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("点单明细", color = CozyCocoa, fontWeight = FontWeight.Black)
                Text("共 ${cart.itemCount} 件", color = CozyMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (cart.isEmpty) {
                Text("还没有选择菜品", color = CozyMuted, style = MaterialTheme.typography.bodyMedium)
            } else {
                cart.items.forEachIndexed { index, item ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CozyRose.copy(alpha = 0.08f))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.menuItemName, color = CozyCocoa, fontWeight = FontWeight.SemiBold)
                            Text("x${item.quantity}", color = CozyMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = yuanText(item.unitPrice * item.quantity),
                            color = CozyCocoa,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CozyRose.copy(alpha = 0.12f))
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("商品小计", color = CozyMuted)
                Text(yuanText(cart.subtotal), color = CozyMuted)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("合计", color = CozyCocoa, fontWeight = FontWeight.Black)
                Text(
                    text = yuanText(cart.totalPrice),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun CheckoutCandyCoinsCard(
    balance: Int,
    cost: Int,
    isCartEmpty: Boolean
) {
    val enough = isCartEmpty || balance >= cost
    CozyCard(
        containerColor = Color(0xFFFFFCF8).copy(alpha = 0.72f),
        borderColor = if (enough) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.error.copy(alpha = 0.34f),
        radius = 28,
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text("糖糖币", color = CozyCocoa, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (isCartEmpty) {
                        "点菜会消耗糖糖币，它不是金币，只是你们的小额度"
                    } else if (enough) {
                        "本次点菜将消耗 $cost 糖糖币"
                    } else {
                        "还差 ${cost - balance} 糖糖币，找饲养员撒点糖"
                    },
                    color = if (enough) CozyMuted else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (enough) Color(0xFFFFD1DC).copy(alpha = 0.68f) else MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CandyCoinIcon(modifier = Modifier.size(22.dp))
                    Text(
                        text = "$balance 枚",
                        color = if (enough) CozyRose else MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutBottomAction(
    cart: CartState,
    candyCoins: Int,
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF8F2).copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        val cost = candyCoinsCost(cart.totalPrice)
        val enabled = !cart.isEmpty && !isSubmitting && candyCoins >= cost
        Surface(
            onClick = onSubmit,
            enabled = enabled,
            shape = RoundedCornerShape(999.dp),
            color = if (enabled) Color(0xFFFF9FB7) else Color(0xFFE7E2DC),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSubmitting) "提交中..." else "提交点菜 · 消耗 $cost 糖糖币",
                    color = Color(0xFFFFFCF8),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
