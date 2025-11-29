package com.fast.smdproject

import android.graphics.Bitmap

data class DownloadsModel (
    val dishName: String = "",
    val dishCategory: String = "",
    val dishDescription: String = "",
    val dishImage: Bitmap? = null,
    val likeIconType : Int,
    val downloadIconType : Int,
    val timerIcon : Int
)