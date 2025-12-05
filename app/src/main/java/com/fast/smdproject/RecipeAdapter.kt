package com.fast.smdproject

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button // <--- Make sure to import this
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecipeAdapter(
    private val context: Context,
    private val recipeList: List<Recipe>,
    private val ipAddress: String
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRecipe: ImageView = itemView.findViewById(R.id.item_image)
        val txtTitle: TextView = itemView.findViewById(R.id.item_title)
        val txtTags: TextView = itemView.findViewById(R.id.item_tags)
        val txtDesc: TextView = itemView.findViewById(R.id.item_desc)

        // 1. Add reference to the button
        val btnDetails: Button = itemView.findViewById(R.id.btn_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_card, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]

        holder.txtTitle.text = recipe.title
        holder.txtTags.text = "Tags: ${recipe.tags}"
        holder.txtDesc.text = recipe.description

        val fullImageUrl = "http://$ipAddress/cookMate/${recipe.imagePath}"

        Glide.with(context)
            .load(fullImageUrl)
            .placeholder(R.drawable.logo2)
            .error(R.drawable.logo2)
            .into(holder.imgRecipe)


        holder.btnDetails.setOnClickListener {
            val intent = Intent(context, RecipeDetail::class.java)
            intent.putExtra("RECIPE_ID", recipe.recipeId)
            context.startActivity(intent)
        }


        holder.itemView.setOnClickListener(null)
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }
}