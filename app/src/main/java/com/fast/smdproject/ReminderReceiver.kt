package com.fast.smdproject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val recipeId = intent.getIntExtra("RECIPE_ID", 0)
        val recipeTitle = intent.getStringExtra("RECIPE_TITLE") ?: "Recipe"

        showNotification(context, recipeId, recipeTitle)
    }

    private fun showNotification(context: Context, recipeId: Int, recipeTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "recipe_reminder_channel"
        val channelName = "Recipe Reminders"

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for recipe cooking reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the app when notification is clicked
        val notificationIntent = Intent(context, DownloadsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            recipeId,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.grass)
            .setContentTitle("Time to cook!")
            .setContentText("It's time to prepare $recipeTitle")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(recipeId, notification)
    }
}

