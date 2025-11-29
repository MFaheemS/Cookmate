package com.fast.smdproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


class DownloadsAdapter(c : Context, data : ArrayList<DownloadsModel>) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

    var context = c
    var dataSet = data

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        var itemView = LayoutInflater.from(context).inflate(R.layout.row_downloads_favorites
            ,parent,false)

        var viewHolder = ViewHolder(itemView)

        return viewHolder
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val data = dataSet[position]

        holder.dishName.text = data.dishName
        holder.dishCategory.text = data.dishCategory
        holder.dishDescription.text = data.dishDescription

        if(data.dishImage == null){

            holder.dishImage.setImageResource(R.drawable.default_food_image)
        }

        else{

            holder.dishImage.setImageBitmap(data.dishImage)
        }

        holder.likeIcon.setImageResource(data.likeIconType)
        holder.downloadIcon.setImageResource(data.downloadIconType)
        holder.timerIcon.setImageResource(data.timerIcon)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    class ViewHolder(itemRow : View) : RecyclerView.ViewHolder(itemRow){

        val dishName = itemRow.findViewById<android.widget.TextView>(R.id.dish_name)
        val dishCategory = itemRow.findViewById<android.widget.TextView>(R.id.dish_category)
        val dishDescription = itemRow.findViewById<android.widget.TextView>(R.id.dish_description)
        val dishImage = itemRow.findViewById<android.widget.ImageView>(R.id.foodImage)
        val likeIcon = itemRow.findViewById<android.widget.ImageView>(R.id.likeButton)
        val downloadIcon = itemRow.findViewById<android.widget.ImageView>(R.id.downloadButton)
        val timerIcon = itemRow.findViewById<android.widget.ImageView>(R.id.timerButton)

    }
}