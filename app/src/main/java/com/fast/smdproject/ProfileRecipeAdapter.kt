package com.fast.smdproject

import android.app.AlertDialog
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

class ProfileRecipeAdapter(
    private val context: Context,
    private val recipes: MutableList<Recipe>,
    private val ipAddress: String,
    private val onRecipeDeleted: () -> Unit
) : RecyclerView.Adapter<ProfileRecipeAdapter.ProfileRecipeViewHolder>() {

    class ProfileRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRecipe: ImageView = itemView.findViewById(R.id.item_image)
        val txtTitle: TextView = itemView.findViewById(R.id.item_title)
        val btnDetails: ImageView = itemView.findViewById(R.id.btn_details)
        val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_recipe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileRecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_recipe, parent, false)
        return ProfileRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileRecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.txtTitle.text = recipe.title

        // Add pop-in animation for items
        com.fast.smdproject.AnimationUtils.popIn(holder.itemView, position * 50L)

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

        // Details button click listener with press effect
        holder.btnDetails.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                val intent = Intent(context, RecipeDetail::class.java)
                intent.putExtra("RECIPE_ID", recipe.recipeId)
                context.startActivity(intent)
            }
        }

        // Delete button click listener with press effect
        holder.btnDelete.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                showDeleteConfirmation(recipe, position, holder)
            }
        }
    }

    override fun getItemCount(): Int = recipes.size

    private fun showDeleteConfirmation(recipe: Recipe, position: Int, holder: ProfileRecipeViewHolder) {
        AlertDialog.Builder(context)
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete \"${recipe.title}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteRecipe(recipe, position, holder)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRecipe(recipe: Recipe, position: Int, holder: ProfileRecipeViewHolder) {
        val db = UserDatabase(context)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Animate item removal immediately for better UX
        com.fast.smdproject.AnimationUtils.fadeOut(holder.itemView, 300) {
            recipes.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, recipes.size)
        }

        val url = "http://$ipAddress/cookMate/delete_recipe.php"

        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        Toast.makeText(context, "Recipe deleted successfully", Toast.LENGTH_SHORT).show()

                        // Callback to refresh profile
                        onRecipeDeleted()
                    } else {
                        val message = json.optString("message", "Failed to delete recipe")
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        // Restore item on failure
                        recipes.add(position, recipe)
                        notifyItemInserted(position)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Parse error: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Restore item on error
                    recipes.add(position, recipe)
                    notifyItemInserted(position)
                }
            },
            { error ->
                Toast.makeText(context, "Network error - action queued", Toast.LENGTH_SHORT).show()
                // Queue action for offline
                db.queueAction(userId, "delete_recipe", recipe.recipeId, "recipe")
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
}

