package com.myorderapp.ui.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.myorderapp.MainActivity
import com.myorderapp.R
import com.myorderapp.domain.model.OrderRecord

private const val CHANNEL_ID = "couple_order_updates"
private const val CHANNEL_NAME = "订单提醒"
const val EXTRA_NOTIFICATION_ORDER_ID = "notification_order_id"

fun notifyActiveOrderIfAllowed(
    context: Context,
    order: OrderRecord,
    isCaretaker: Boolean
) {
    runCatching {
        val manager = NotificationManagerCompat.from(context)
        if (order.status in setOf("completed", "cancelled")) {
            manager.cancel(order.id.hashCode())
            return@runCatching
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return@runCatching
        }

        ensureOrderNotificationChannel(context)
        val title = when (order.status) {
            "submitted", "confirmed" -> if (isCaretaker) "有新的点菜单等你确认" else "等待饲养员确认接单"
            "preparing", "delivering" -> "这顿饭正在准备中"
            else -> "订单有新进展"
        }
        val content = order.items.take(2).joinToString("、") { it.menuItemName }
            .ifBlank { order.shopName.ifBlank { "打开 App 查看详情" } }
        val contentIntent = orderDetailPendingIntent(context, order.id)
        val notification = if (Build.VERSION.SDK_INT >= 36) {
            buildPromotedOrderNotification(context, order, title, content, contentIntent)
        } else {
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_order)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .build()
        }

        manager.notify(order.id.hashCode(), notification)
    }.onFailure { error ->
        Log.w("OrderNotification", "Unable to update order notification", error)
    }
}

private fun orderDetailPendingIntent(context: Context, orderId: String): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("orderdisk://orders/${Uri.encode(orderId)}")
        putExtra(EXTRA_NOTIFICATION_ORDER_ID, orderId)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
        context,
        orderId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

@androidx.annotation.RequiresApi(36)
private fun buildPromotedOrderNotification(
    context: Context,
    order: OrderRecord,
    title: String,
    content: String,
    contentIntent: PendingIntent
): android.app.Notification {
    val progress = when (order.status) {
        "submitted", "confirmed" -> 20
        "preparing", "delivering" -> 65
        "completed" -> 100
        else -> 0
    }
    val isActive = order.status !in setOf("completed", "cancelled")
    val style = android.app.Notification.ProgressStyle()
        .setProgress(progress)
        .setProgressSegments(
            listOf(android.app.Notification.ProgressStyle.Segment(100).setColor(Color.rgb(255, 145, 164)))
        )
        .setStyledByProgress(true)

    return android.app.Notification.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_order)
        .setContentTitle(title)
        .setContentText(content)
        .setContentIntent(contentIntent)
        .setStyle(style)
        .setCategory(android.app.Notification.CATEGORY_PROGRESS)
        .setShortCriticalText(order.shopName.take(7).ifBlank { "高糖小食" })
        .setRequestPromotedOngoing(isActive)
        .setOngoing(isActive)
        .setOnlyAlertOnce(true)
        .setAutoCancel(!isActive)
        .build()
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
