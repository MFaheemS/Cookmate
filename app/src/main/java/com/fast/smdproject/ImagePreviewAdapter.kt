package com.fast.smdproject

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ImagePreviewAdapter(
    private val images: MutableList<Bitmap>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPreview: ImageView = itemView.findViewById(R.id.img_preview)
        val btnRemove: ImageView = itemView.findViewById(R.id.btn_remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_preview, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val bitmap = images[position]
        holder.imgPreview.setImageBitmap(bitmap)

        holder.btnRemove.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount(): Int = images.size
}

