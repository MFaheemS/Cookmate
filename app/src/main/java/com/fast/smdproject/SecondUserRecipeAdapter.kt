package com.fast.smdproject

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONObject

class SecondUserRecipeAdapter(
    private val context: Context,
    private val recipes: MutableList<Recipe>,
    private val ipAddress: String,
    private val currentUserId: Int
) : RecyclerView.Adapter<SecondUserRecipeAdapter.SecondUserRecipeViewHolder>() {

    class SecondUserRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRecipe: ImageView = itemView.findViewById(R.id.item_image)
        val txtTitle: TextView = itemView.findViewById(R.id.item_title)
        val btnDetails: ImageView = itemView.findViewById(R.id.btn_details)
        val iconLike: ImageView = itemView.findViewById(R.id.icon_like)
        val iconDownload: ImageView = itemView.findViewById(R.id.icon_download)
        val iconCheck: ImageView = itemView.findViewById(R.id.icon_check)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SecondUserRecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_second_user_recipe, parent, false)
        return SecondUserRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SecondUserRecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.txtTitle.text = recipe.title

        // Store holder reference for later use
        holder.iconDownload.tag = holder
        holder.iconLike.tag = holder

        // Load recipe image
        if (recipe.imagePath.isNotEmpty()) {
            val imageUrl = "http://$ipAddress/cookMate/${recipe.imagePath}"
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.logo2)
                .error(R.drawable.logo2)
                .into(holder.imgRecipe)
        } else {
            holder.imgRecipe.setImageResource(R.drawable.logo2)
        }

        // Check and set like status
        checkLikeStatus(recipe.recipeId, holder.iconLike)

        // Check and set download status (will also update checkmark)
        checkDownloadStatus(recipe.recipeId, holder.iconDownload)

        // Details button click listener - opens recipe detail
        holder.btnDetails.setOnClickListener {
            val intent = Intent(context, RecipeDetail::class.java)
            intent.putExtra("RECIPE_ID", recipe.recipeId)
            context.startActivity(intent)
        }

        // Like button click listener
        holder.iconLike.setOnClickListener {
            toggleLike(recipe.recipeId, holder.iconLike)
        }

        // Download button click listener
        holder.iconDownload.setOnClickListener {
            toggleDownload(recipe.recipeId, holder.iconDownload, holder)
        }
    }

    override fun getItemCount(): Int = recipes.size

    private fun checkLikeStatus(recipeId: Int, iconLike: ImageView) {
        val url = "http://$ipAddress/cookMate/check_recipe_status.php?user_id=$currentUserId&recipe_id=$recipeId"

        val request = object : StringRequest(
            Method.GET,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val isLiked = json.getBoolean("is_liked")
                        iconLike.setImageResource(
                            if (isLiked) R.drawable.ic_like_fill else R.drawable.ic_like
                        )
                        // Store the state in the tag
                        iconLike.setTag(R.id.icon_like, isLiked)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Silently fail
            }
        ) {}

        Volley.newRequestQueue(context).add(request)
    }

    private fun checkDownloadStatus(recipeId: Int, iconDownload: ImageView) {
        val url = "http://$ipAddress/cookMate/check_recipe_status.php?user_id=$currentUserId&recipe_id=$recipeId"

        val request = object : StringRequest(
            Method.GET,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val isDownloaded = json.getBoolean("is_downloaded")
                        iconDownload.setImageResource(
                            if (isDownloaded) R.drawable.ic_downloaded else R.drawable.ic_download
                        )
                        // Store the state in the tag
                        iconDownload.setTag(R.id.icon_download, isDownloaded)

                        // Show/hide checkmark based on download status
                        val holder = iconDownload.tag as? SecondUserRecipeViewHolder
                        holder?.iconCheck?.visibility = if (isDownloaded) View.VISIBLE else View.GONE
                        holder?.btnDetails?.visibility = if (isDownloaded) View.GONE else View.VISIBLE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Silently fail
            }
        ) {}

        Volley.newRequestQueue(context).add(request)
    }

    private fun toggleLike(recipeId: Int, iconLike: ImageView) {
        // Get current status from tag (default to false if not set)
        val isCurrentlyLiked = iconLike.getTag(R.id.icon_like) as? Boolean ?: false

        val url = if (isCurrentlyLiked) {
            "http://$ipAddress/cookMate/unlike_recipe.php"
        } else {
            "http://$ipAddress/cookMate/like_recipe.php"
        }

        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        // Toggle icon
                        val newState = !isCurrentlyLiked
                        iconLike.setImageResource(
                            if (newState) R.drawable.ic_like_fill else R.drawable.ic_like
                        )
                        // Update the stored state
                        iconLike.setTag(R.id.icon_like, newState)

                        val message = if (isCurrentlyLiked) "Removed from favorites" else "Added to favorites"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = currentUserId.toString()
                params["recipe_id"] = recipeId.toString()
                return params
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun toggleDownload(recipeId: Int, iconDownload: ImageView, holder: SecondUserRecipeViewHolder) {
        // Get current status from tag (default to false if not set)
        val isCurrentlyDownloaded = iconDownload.getTag(R.id.icon_download) as? Boolean ?: false

        val url = if (isCurrentlyDownloaded) {
            "http://$ipAddress/cookMate/remove_download.php"
        } else {
            "http://$ipAddress/cookMate/download_recipe.php"
        }

        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        // Toggle icon
                        val newState = !isCurrentlyDownloaded
                        iconDownload.setImageResource(
                            if (newState) R.drawable.ic_downloaded else R.drawable.ic_download
                        )
                        // Update the stored state
                        iconDownload.setTag(R.id.icon_download, newState)

                        // Toggle checkmark and details button visibility
                        holder.iconCheck.visibility = if (newState) View.VISIBLE else View.GONE
                        holder.btnDetails.visibility = if (newState) View.GONE else View.VISIBLE

                        val message = if (isCurrentlyDownloaded) "Removed from downloads" else "Added to downloads"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = currentUserId.toString()
                params["recipe_id"] = recipeId.toString()
                return params
            }
        }

        Volley.newRequestQueue(context).add(request)
    }
}

