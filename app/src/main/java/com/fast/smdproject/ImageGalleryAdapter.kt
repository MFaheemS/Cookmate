package com.fast.smdproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageGalleryAdapter(
    private val images: List<String>,
    private val ipAddress: String
) : RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.img_gallery_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imagePath = images[position % images.size] // Loop images
        val imageUrl = "http://$ipAddress/cookMate/$imagePath"

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.logo2)
            .error(R.drawable.logo2)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = Int.MAX_VALUE // Infinite loop
}

