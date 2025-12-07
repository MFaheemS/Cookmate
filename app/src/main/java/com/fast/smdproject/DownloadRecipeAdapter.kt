package com.fast.smdproject

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DownloadRecipeAdapter(
    private val context: Context,
    private val recipeList: List<Recipe>,
    private val ipAddress: String,
    private val downloadTimes: Map<Int, String> = emptyMap()
) : RecyclerView.Adapter<DownloadRecipeAdapter.DownloadViewHolder>() {

    class DownloadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRecipe: ImageView = itemView.findViewById(R.id.item_image)
        val txtTitle: TextView = itemView.findViewById(R.id.item_title)
        val txtTags: TextView = itemView.findViewById(R.id.item_tags)
        val txtDesc: TextView = itemView.findViewById(R.id.item_desc)
        val txtLikeCount: TextView = itemView.findViewById(R.id.like_count)
        val txtDownloadCount: TextView = itemView.findViewById(R.id.download_count)
        val txtDownloadTime: TextView = itemView.findViewById(R.id.download_time)
        val iconLike: ImageView = itemView.findViewById(R.id.icon_like)
        val iconDownload: ImageView = itemView.findViewById(R.id.icon_download)
        val iconClock: ImageView = itemView.findViewById(R.id.icon_clock)
        val btnDetails: Button = itemView.findViewById(R.id.btn_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download_card, parent, false)
        return DownloadViewHolder(view)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        val recipe = recipeList[position]

        val db = UserDatabase(context)
        val currentUserId = db.getCurrentUserId()
        val isOwnRecipe = recipe.ownerId == currentUserId && currentUserId != 0

        holder.txtTitle.text = recipe.title
        holder.txtTags.text = recipe.tags
        holder.txtDesc.text = recipe.description
        holder.txtLikeCount.text = recipe.likeCount.toString()
        holder.txtDownloadCount.text = recipe.downloadCount.toString()

        // Calculate and display time since download
        val downloadTime = downloadTimes[recipe.recipeId]
        if (downloadTime != null) {
            holder.txtDownloadTime.text = getTimeAgo(downloadTime)
        } else {
            holder.txtDownloadTime.text = "Now"
        }

        // Set initial icon states
        updateLikeIcon(holder.iconLike, recipe.isLiked)
        updateDownloadIcon(holder.iconDownload, recipe.isDownloaded)

        // Disable and grey out icons if it's user's own recipe
        if (isOwnRecipe) {
            holder.iconLike.alpha = 0.3f
            holder.iconDownload.alpha = 0.3f
            holder.iconLike.isEnabled = false
            holder.iconDownload.isEnabled = false
        } else {
            holder.iconLike.alpha = 1.0f
            holder.iconDownload.alpha = 1.0f
            holder.iconLike.isEnabled = true
            holder.iconDownload.isEnabled = true
        }

        val fullImageUrl = "http://$ipAddress/cookMate/${recipe.imagePath}"

        Glide.with(context)
            .load(fullImageUrl)
            .placeholder(R.drawable.logo2)
            .error(R.drawable.logo2)
            .into(holder.imgRecipe)

        // Like button click - disabled for own recipes
        holder.iconLike.setOnClickListener {
            if (!isOwnRecipe) {
                toggleLike(recipe, holder)
            } else {
                Toast.makeText(context, "You cannot like your own recipe", Toast.LENGTH_SHORT).show()
            }
        }

        // Download button click - disabled for own recipes
        holder.iconDownload.setOnClickListener {
            if (!isOwnRecipe) {
                toggleDownload(recipe, holder)
            } else {
                Toast.makeText(context, "You cannot download your own recipe", Toast.LENGTH_SHORT).show()
            }
        }

        // Clock icon click - open set timer activity
        holder.iconClock.setOnClickListener {
            val intent = Intent(context, SetTimerActivity::class.java)
            intent.putExtra("RECIPE_ID", recipe.recipeId)
            intent.putExtra("RECIPE_TITLE", recipe.title)
            intent.putExtra("RECIPE_IMAGE", recipe.imagePath)
            context.startActivity(intent)
        }

        holder.btnDetails.setOnClickListener {
            val intent = Intent(context, RecipeDetail::class.java)
            intent.putExtra("RECIPE_ID", recipe.recipeId)
            context.startActivity(intent)
        }
    }

    private fun getTimeAgo(timestamp: String): String {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(timestamp)
            val now = Date()
            val diff = now.time - (date?.time ?: 0)

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Now"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h"
                diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d"
                else -> "${TimeUnit.MILLISECONDS.toDays(diff) / 7}w"
            }
        } catch (e: Exception) {
            return "Now"
        }
    }

    private fun toggleLike(recipe: Recipe, holder: DownloadViewHolder) {
        val db = UserDatabase(context)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val endpoint = if (recipe.isLiked) "unlike_recipe.php" else "like_recipe.php"
        val url = "http://$ipAddress/cookMate/$endpoint"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        recipe.isLiked = !recipe.isLiked
                        recipe.likeCount = json.getInt("like_count")

                        animateIconChange(holder.iconLike) {
                            updateLikeIcon(holder.iconLike, recipe.isLiked)
                        }
                        holder.txtLikeCount.text = recipe.likeCount.toString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId.toString()
                params["recipe_id"] = recipe.recipeId.toString()
                return params
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun toggleDownload(recipe: Recipe, holder: DownloadViewHolder) {
        val db = UserDatabase(context)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val endpoint = if (recipe.isDownloaded) "remove_download.php" else "download_recipe.php"
        val url = "http://$ipAddress/cookMate/$endpoint"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val wasDownloaded = recipe.isDownloaded
                        recipe.isDownloaded = !recipe.isDownloaded
                        recipe.downloadCount = json.getInt("download_count")

                        // Update local database
                        if (recipe.isDownloaded) {
                            // Save to local database
                            val downloadedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            db.saveDownloadedRecipe(recipe, userId, downloadedAt)
                        } else {
                            // Remove from local database
                            db.deleteDownloadedRecipe(recipe.recipeId, userId)
                        }

                        animateIconChange(holder.iconDownload) {
                            updateDownloadIcon(holder.iconDownload, recipe.isDownloaded)
                        }
                        holder.txtDownloadCount.text = recipe.downloadCount.toString()

                        if (!recipe.isDownloaded && wasDownloaded) {
                            Toast.makeText(context, "Removed from downloads", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId.toString()
                params["recipe_id"] = recipe.recipeId.toString()
                return params
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun animateIconChange(icon: ImageView, onComplete: () -> Unit) {
        val fadeOut = ObjectAnimator.ofFloat(icon, "alpha", 1f, 0f)
        fadeOut.duration = 150
        fadeOut.interpolator = AccelerateDecelerateInterpolator()

        fadeOut.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onComplete()
                val fadeIn = ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f)
                fadeIn.duration = 150
                fadeIn.interpolator = AccelerateDecelerateInterpolator()
                fadeIn.start()
            }
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })

        fadeOut.start()
    }

    private fun updateLikeIcon(icon: ImageView, isLiked: Boolean) {
        icon.setImageResource(if (isLiked) R.drawable.ic_like_fill else R.drawable.ic_like)
    }

    private fun updateDownloadIcon(icon: ImageView, isDownloaded: Boolean) {
        icon.setImageResource(if (isDownloaded) R.drawable.ic_downloaded else R.drawable.ic_download)
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }
}

