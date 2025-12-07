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

        // Check notification settings
        val db = UserDatabase(context)
        val allowNotifications = db.allowNotifications()
        val allowRecipeNotifications = db.allowRecipeNotifications()

        // Only show notification if both settings are enabled
        if (allowNotifications && allowRecipeNotifications) {
            showNotification(context, recipeId, recipeTitle)
        }
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
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the reminder activity
        val notificationIntent = Intent(context, ReminderActivity::class.java).apply {
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
            .setContentTitle("⏰ Time to cook!")
            .setContentText("It's time to prepare $recipeTitle")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("It's time to prepare $recipeTitle. Check your reminder for details!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .build()

        notificationManager.notify(recipeId, notification)
    }
}

