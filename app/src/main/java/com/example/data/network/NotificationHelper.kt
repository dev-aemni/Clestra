package com.example.data.network

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "clestra_messages"
    private const val CHANNEL_NAME = "Clestra Messages"
    private const val CHANNEL_DESC = "Notifications for incoming P2P, relay, and Clestra messages"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun isDeviceOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun showMessageNotification(
        context: Context,
        peerId: String,
        senderName: String,
        messageText: String,
        isOfficial: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_peer_id", peerId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            peerId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        fun buildSoftRemoteViews(maxLines: Int): RemoteViews =
            RemoteViews(context.packageName, R.layout.notification_clestra_soft).apply {
                setTextViewText(R.id.notif_sender_name, senderName)
                setTextViewText(R.id.notif_time, timeFormatted)
                setTextViewText(R.id.notif_message_text, messageText)
                setInt(R.id.notif_message_text, "setMaxLines", maxLines)
                if (isOfficial) {
                    setTextViewText(R.id.notif_badge, "OFFICIAL")
                    setViewVisibility(R.id.notif_badge, View.VISIBLE)
                } else {
                    setViewVisibility(R.id.notif_badge, View.GONE)
                }
            }

        val collapsedViews = buildSoftRemoteViews(maxLines = 2)
        val expandedViews = buildSoftRemoteViews(maxLines = 5)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setColor(0xFFC4763A.toInt())
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(peerId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }
}
