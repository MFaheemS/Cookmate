package com.fast.smdproject

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.json.JSONArray

class SearchRecipeAdapter(
    private val context: Context,
    private val recipes: MutableList<Recipe>,
    private val ipAddress: String,
    private val searchCategories: List<String> = emptyList()
) : RecyclerView.Adapter<SearchRecipeAdapter.SearchRecipeViewHolder>() {

    class SearchRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRecipe: ImageView = itemView.findViewById(R.id.recipe_image)
        val txtTitle: TextView = itemView.findViewById(R.id.recipe_title)
        val txtCategories: TextView = itemView.findViewById(R.id.recipe_categories)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchRecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_recipe_result, parent, false)
        return SearchRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchRecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.txtTitle.text = recipe.title

        // Add slide-up fade-in animation with stagger
        com.fast.smdproject.AnimationUtils.slideUpFadeIn(holder.itemView, position * 30L)

        // Parse categories from tags (format: #lunch, #spicy)
        val categories = recipe.tags
            .split(",")
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }

        // Build categories text with highlighting
        val categoriesText = buildCategoriesText(categories)
        holder.txtCategories.text = categoriesText

        // Load recipe image
        val coverImage = try {
            val imagesArray = JSONArray(recipe.imagePath)
            if (imagesArray.length() > 0) {
                imagesArray.getString(0)
            } else {
                recipe.imagePath
            }
        } catch (e: Exception) {
            recipe.imagePath
        }

        if (coverImage.isNotEmpty()) {
            val imageUrl = "http://$ipAddress/cookMate/$coverImage"
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.logo2)
                .error(R.drawable.logo2)
                .into(holder.imgRecipe)
        } else {
            holder.imgRecipe.setImageResource(R.drawable.logo2)
        }

        // Click to open recipe detail with scale animation
        holder.itemView.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                val intent = Intent(context, RecipeDetail::class.java)
                intent.putExtra("RECIPE_ID", recipe.recipeId)
                context.startActivity(intent)
                (context as? android.app.Activity)?.overridePendingTransition(
                    R.anim.activity_enter,
                    R.anim.activity_exit
                )
            }
        }
    }

    override fun getItemCount(): Int = recipes.size

    private fun buildCategoriesText(categories: List<String>): SpannableString {
        val categoriesStr = "Categories: " + categories.joinToString(", ")
        val spannable = SpannableString(categoriesStr)

        // Highlight matching categories in bold
        if (searchCategories.isNotEmpty()) {
            categories.forEachIndexed { index, category ->
                // Check if this category matches any search category (case-insensitive)
                if (searchCategories.any { it.equals(category, ignoreCase = true) }) {
                    // Find the position of this category in the full text
                    val startIndex = categoriesStr.indexOf(category, startIndex = 12) // After "Categories: "
                    if (startIndex != -1) {
                        spannable.setSpan(
                            StyleSpan(Typeface.BOLD),
                            startIndex,
                            startIndex + category.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
        }

        return spannable
    }
}

