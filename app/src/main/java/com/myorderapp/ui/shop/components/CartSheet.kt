package com.myorderapp.ui.shop.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myorderapp.domain.model.CartState
import com.myorderapp.ui.components.OrderCard
import com.myorderapp.ui.components.OrderPrimaryButton
import com.myorderapp.ui.components.OrderTertiaryButton
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (cartState.shopName.isBlank()) "购物车" else cartState.shopName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cartState.items, key = { it.id }) { item ->
                OrderCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.menuItemName, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                yuanText(item.unitPrice),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OrderTertiaryButton(
                                icon = Icons.Default.Remove,
                                contentDescription = "减少",
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { onDecrease(item.menuItemId) }
                            )
                            Text(item.quantity.toString(), fontWeight = FontWeight.Bold)
                            OrderTertiaryButton(
                                icon = Icons.Default.Add,
                                contentDescription = "增加",
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { onIncrease(item.menuItemId) }
                            )
                        }
                    }
                }
            }
        }

        OrderCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("小计")
                    Text(yuanText(cartState.subtotal), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("配送费")
                    Text(yuanText(cartState.deliveryFee), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("合计", fontWeight = FontWeight.Bold)
                    Text(yuanText(cartState.totalPrice), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!cartState.isEmpty) {
            OrderPrimaryButton(
                text = "去结算",
                modifier = Modifier.fillMaxWidth(),
                onClick = onCheckout
            )
        }
    }
}
