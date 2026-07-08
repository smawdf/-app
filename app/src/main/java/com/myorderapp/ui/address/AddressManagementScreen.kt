package com.myorderapp.ui.address

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddressManagementScreen(
    viewModel: AddressViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddAddressDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, line1, line2, tag ->
                viewModel.addAddress(name, phone, line1, line2, tag)
                showAddDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("收货地址", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("添加地址")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(uiState.addresses, key = { it.id }) { address ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = buildString {
                                    append(address.contactName)
                                    if (address.isDefault) append(" · 默认")
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Text(address.contactPhone)
                            Text("${address.addressLine1} ${address.addressLine2}".trim())
                            if (address.tag.isNotBlank()) {
                                Text(address.tag, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteAddress(address.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAddressDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var addressLine1 by remember { mutableStateOf("") }
    var addressLine2 by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    val canSave = name.isNotBlank() && phone.isNotBlank() && addressLine1.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加地址") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("联系人") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("手机号") }, singleLine = true)
                OutlinedTextField(value = addressLine1, onValueChange = { addressLine1 = it }, label = { Text("地址") }, singleLine = true)
                OutlinedTextField(value = addressLine2, onValueChange = { addressLine2 = it }, label = { Text("门牌号，可选") }, singleLine = true)
                OutlinedTextField(value = tag, onValueChange = { tag = it }, label = { Text("标签，可选") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, addressLine1, addressLine2, tag) },
                enabled = canSave
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
