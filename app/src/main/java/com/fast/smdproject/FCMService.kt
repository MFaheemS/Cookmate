package com.fast.smdproject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            val title = it.title ?: "CookMate"
            val body = it.body ?: ""
            sendNotification(title, body, remoteMessage.data)
        }

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataPayload(remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // Send token to your server
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val db = UserDatabase(this)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            // Save token locally to send later when user logs in
            val prefs = getSharedPreferences("FCM", Context.MODE_PRIVATE)
            prefs.edit().putString("pending_token", token).apply()
            return
        }

        // Send to server
        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/update_fcm_token.php"

        val request = object : com.android.volley.toolbox.StringRequest(
            Method.POST,
            url,
            { response ->
                Log.d(TAG, "Token sent to server successfully")
            },
            { error ->
                Log.e(TAG, "Failed to send token to server: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId.toString()
                params["fcm_token"] = token
                return params
            }
        }

        com.android.volley.toolbox.Volley.newRequestQueue(this).add(request)
    }

    private fun handleDataPayload(data: Map<String, String>) {
        val type = data["type"] ?: return
        val title = data["title"] ?: "CookMate"
        val body = data["body"] ?: ""

        when (type) {
            "new_follower" -> {
                sendNotification(title, body, data, HomePage::class.java)
            }
            "new_recipe" -> {
                val recipeId = data["recipe_id"]?.toIntOrNull()
                if (recipeId != null) {
                    sendNotification(title, body, data, RecipeDetail::class.java, recipeId)
                }
            }
            "reminder" -> {
                sendNotification(title, body, data, ReminderActivity::class.java)
            }
            else -> {
                sendNotification(title, body, data)
            }
        }
    }

    private fun sendNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        targetActivity: Class<*>? = null,
        recipeId: Int? = null
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = when (data["type"]) {
            "new_follower" -> CHANNEL_ID_FOLLOWERS
            "new_recipe" -> CHANNEL_ID_RECIPES
            "reminder" -> CHANNEL_ID_REMINDERS
            else -> CHANNEL_ID_GENERAL
        }

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = when (channelId) {
                CHANNEL_ID_FOLLOWERS -> "Followers"
                CHANNEL_ID_RECIPES -> "New Recipes"
                CHANNEL_ID_REMINDERS -> "Reminders"
                else -> "General"
            }

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for $channelName"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent
        val intent = when (targetActivity) {
            null -> Intent(this, HomePage::class.java)
            else -> Intent(this, targetActivity)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            recipeId?.let { putExtra("RECIPE_ID", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.grass)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID_GENERAL = "general_notifications"
        private const val CHANNEL_ID_FOLLOWERS = "follower_notifications"
        private const val CHANNEL_ID_RECIPES = "recipe_notifications"
        private const val CHANNEL_ID_REMINDERS = "reminder_notifications"
    }
}

