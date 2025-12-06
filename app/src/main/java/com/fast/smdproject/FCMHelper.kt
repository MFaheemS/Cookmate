package com.fast.smdproject

import android.content.Context
import android.util.Log
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.messaging.FirebaseMessaging

object FCMHelper {

    private const val TAG = "FCMHelper"

    fun initializeFCM(context: Context) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            // Send token to server
            sendTokenToServer(context, token)
        }
    }

    fun sendTokenToServer(context: Context, token: String) {
        val db = UserDatabase(context)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            // Save token to send later when user logs in
            val prefs = context.getSharedPreferences("FCM", Context.MODE_PRIVATE)
            prefs.edit().putString("pending_token", token).apply()
            Log.d(TAG, "User not logged in, token saved locally")
            return
        }

        val ipAddress = context.getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/update_fcm_token.php"

        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                Log.d(TAG, "Token sent to server: $response")
            },
            { error ->
                Log.e(TAG, "Failed to send token: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId.toString()
                params["fcm_token"] = token
                return params
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    fun sendPendingToken(context: Context) {
        val prefs = context.getSharedPreferences("FCM", Context.MODE_PRIVATE)
        val pendingToken = prefs.getString("pending_token", null)

        if (pendingToken != null) {
            sendTokenToServer(context, pendingToken)
            // Clear pending token
            prefs.edit().remove("pending_token").apply()
        }
    }
}

