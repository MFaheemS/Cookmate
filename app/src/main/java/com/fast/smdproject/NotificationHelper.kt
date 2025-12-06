package com.fast.smdproject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID_FOLLOWER = "follower_notifications"
    private const val CHANNEL_ID_RECIPE = "recipe_notifications"

    fun showFollowerNotification(context: Context, followerUsername: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_FOLLOWER,
                "New Followers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when someone follows you"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open profile/followers
        val intent = Intent(context, UserProfileActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FOLLOWER)
            .setSmallIcon(R.drawable.grass)
            .setContentTitle("New Follower")
            .setContentText("$followerUsername started following you!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$followerUsername started following you! Check out their profile."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(500, 500))
            .build()

        notificationManager.notify(followerUsername.hashCode(), notification)
    }

    fun showNewRecipeNotification(context: Context, authorUsername: String, recipeTitle: String, recipeId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_RECIPE,
                "New Recipes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when someone you follow uploads a recipe"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open recipe detail
        val intent = Intent(context, RecipeDetail::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("RECIPE_ID", recipeId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            recipeId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_RECIPE)
            .setSmallIcon(R.drawable.grass)
            .setContentTitle("New Recipe from $authorUsername")
            .setContentText(recipeTitle)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$authorUsername just uploaded a new recipe: $recipeTitle. Tap to view!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(500, 500))
            .build()

        notificationManager.notify(recipeId, notification)
    }
}

