package com.fast.smdproject

import android.content.Context
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class ActionQueueService(private val context: Context) {

    fun processPendingActions(onComplete: (() -> Unit)? = null) {
        val db = UserDatabase(context)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            onComplete?.invoke()
            return
        }

        val pendingActions = db.getPendingActions(userId)

        if (pendingActions.isEmpty()) {
            onComplete?.invoke()
            return
        }

        var processedCount = 0
        val totalActions = pendingActions.size

        pendingActions.forEach { action ->
            processAction(action, userId, db) { success ->
                if (success) {
                    val actionId = action["id"]?.toIntOrNull() ?: 0
                    db.deletePendingAction(actionId)
                } else {
                    val actionId = action["id"]?.toIntOrNull() ?: 0
                    db.incrementActionAttempts(actionId)
                }

                processedCount++
                if (processedCount == totalActions) {
                    onComplete?.invoke()
                }
            }
        }

        // Clean up old actions (older than 7 days)
        db.clearOldPendingActions(userId, 7)
    }

    private fun processAction(
        action: HashMap<String, String>,
        userId: Int,
        db: UserDatabase,
        onComplete: (Boolean) -> Unit
    ) {
        val actionType = action["action_type"] ?: ""
        val targetId = action["target_id"]?.toIntOrNull() ?: 0
        val targetType = action["target_type"] ?: ""
        val ipAddress = context.getString(R.string.ipAddress)

        when (actionType) {
            "like" -> processLikeAction(ipAddress, userId, targetId, true, onComplete)
            "unlike" -> processLikeAction(ipAddress, userId, targetId, false, onComplete)
            "download" -> processDownloadAction(ipAddress, userId, targetId, true, onComplete)
            "undownload" -> processDownloadAction(ipAddress, userId, targetId, false, onComplete)
            "follow" -> processFollowAction(ipAddress, userId, targetId, true, onComplete)
            "unfollow" -> processFollowAction(ipAddress, userId, targetId, false, onComplete)
            "delete_recipe" -> processDeleteRecipeAction(ipAddress, userId, targetId, onComplete)
            "set_reminder" -> {
                val reminderTime = action["additional_data"]?.toLongOrNull() ?: 0
                processSetReminderAction(ipAddress, userId, targetId, reminderTime, onComplete)
            }
            "delete_reminder" -> processDeleteReminderAction(ipAddress, userId, targetId, onComplete)
            else -> onComplete(false)
        }
    }

    private fun processLikeAction(ipAddress: String, userId: Int, recipeId: Int, isLike: Boolean, onComplete: (Boolean) -> Unit) {
        val endpoint = if (isLike) "like_recipe.php" else "unlike_recipe.php"
        val url = "http://$ipAddress/cookMate/$endpoint"

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    onComplete(json.getString("status") == "success")
                } catch (e: Exception) {
                    onComplete(false)
                }
            },
            { onComplete(false) }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "recipe_id" to recipeId.toString()
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun processDownloadAction(ipAddress: String, userId: Int, recipeId: Int, isDownload: Boolean, onComplete: (Boolean) -> Unit) {
        val endpoint = if (isDownload) "download_recipe.php" else "remove_download.php"
        val url = "http://$ipAddress/cookMate/$endpoint"

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    onComplete(json.getString("status") == "success")
                } catch (e: Exception) {
                    onComplete(false)
                }
            },
            { onComplete(false) }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "recipe_id" to recipeId.toString()
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun processFollowAction(ipAddress: String, userId: Int, targetUserId: Int, isFollow: Boolean, onComplete: (Boolean) -> Unit) {
        val endpoint = if (isFollow) "follow_user.php" else "unfollow_user.php"
        val url = "http://$ipAddress/cookMate/$endpoint"

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    onComplete(json.getString("status") == "success")
                } catch (e: Exception) {
                    onComplete(false)
                }
            },
            { onComplete(false) }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "target_user_id" to targetUserId.toString()
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun processDeleteRecipeAction(ipAddress: String, userId: Int, recipeId: Int, onComplete: (Boolean) -> Unit) {
        val url = "http://$ipAddress/cookMate/delete_recipe.php"

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    onComplete(json.getString("status") == "success")
                } catch (e: Exception) {
                    onComplete(false)
                }
            },
            { onComplete(false) }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "recipe_id" to recipeId.toString()
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun processSetReminderAction(ipAddress: String, userId: Int, recipeId: Int, reminderTime: Long, onComplete: (Boolean) -> Unit) {
        val url = "http://$ipAddress/cookMate/save_reminder.php"

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    onComplete(json.getString("status") == "success")
                } catch (e: Exception) {
                    onComplete(false)
                }
            },
            { onComplete(false) }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "recipe_id" to recipeId.toString(),
                    "reminder_time" to reminderTime.toString()
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun processDeleteReminderAction(ipAddress: String, userId: Int, recipeId: Int, onComplete: (Boolean) -> Unit) {
        val url = "http://$ipAddress/cookMate/delete_reminder.php"

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    onComplete(json.getString("status") == "success")
                } catch (e: Exception) {
                    onComplete(false)
                }
            },
            { onComplete(false) }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "recipe_id" to recipeId.toString()
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }
}

