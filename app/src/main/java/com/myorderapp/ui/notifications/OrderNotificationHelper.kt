package com.myorderapp.ui.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.myorderapp.R
import com.myorderapp.domain.model.OrderRecord

private const val CHANNEL_ID = "couple_order_updates"
private const val CHANNEL_NAME = "订单提醒"

fun notifyActiveOrderIfAllowed(
    context: Context,
    order: OrderRecord,
    isCaretaker: Boolean
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val manager = NotificationManagerCompat.from(context)
    ensureOrderNotificationChannel(context)
    val title = when (order.status) {
        "submitted" -> if (isCaretaker) "有新的点菜单等你接单" else "订单已提交"
        "confirmed" -> "饲养员已接单"
        "delivering" -> "这顿饭正在准备中"
        else -> "订单有新进展"
    }
    val content = order.items.take(2).joinToString("、") { it.menuItemName }
        .ifBlank { order.shopName.ifBlank { "打开 App 查看详情" } }
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(content)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    manager.notify(order.id.hashCode(), notification)
}

private fun ensureOrderNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "伴侣点菜和订单状态变化提醒"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}
