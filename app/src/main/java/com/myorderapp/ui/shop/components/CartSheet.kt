package com.myorderapp.ui.shop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun CartSheet(
    cartState: CartState,
    onDecrease: (String) -> Unit,
    onIncrease: (String) -> Unit,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF8F2))
            .navigationBarsPadding()
            .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = Color(0xFFFFD1DC).copy(alpha = 0.64f)) {
                Icon(
                    Icons.Filled.ShoppingBasket,
                    contentDescription = null,
                    tint = CozyRose,
                    modifier = Modifier.padding(12.dp).size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (cartState.shopName.isBlank()) "购物篮" else cartState.shopName,
                    color = CozyCocoa,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text("确认购物篮后进入确认点菜", color = CozyMuted, style = MaterialTheme.typography.bodySmall)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(cartState.items, key = { it.id }) { item ->
                CozyCard(containerColor = Color(0xFFFFFCF8).copy(alpha = 0.76f), radius = 24) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.menuItemName, color = CozyCocoa, fontWeight = FontWeight.Black)
                            Text(yuanText(item.unitPrice), color = CozyRose, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StepperButton(Icons.Default.Remove, "减少") { onDecrease(item.menuItemId) }
                            Text(item.quantity.toString(), color = CozyCocoa, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp))
                            StepperButton(Icons.Default.Add, "增加") { onIncrease(item.menuItemId) }
                        }
                    }
                }
            }
        }

        CozyCard(containerColor = Color(0xFFFFFCF8).copy(alpha = 0.82f), radius = 28) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("小计", color = CozyMuted)
                    Text(yuanText(cartState.subtotal), color = CozyCocoa, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("合计", color = CozyCocoa, fontWeight = FontWeight.Black)
                    Text(yuanText(cartState.totalPrice), color = CozyRose, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(4.dp))
                CozyPrimaryButton(
                    text = "去结算",
                    onClick = onCheckout,
                    enabled = !cartState.isEmpty,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = CircleShape, color = Color(0xFFFFD1DC).copy(alpha = 0.72f)) {
        Icon(icon, contentDescription = contentDescription, tint = CozyRose, modifier = Modifier.padding(8.dp).size(18.dp))
    }
}
