package com.fast.smdproject

data class Recipe(
    val recipeId: Int,
    val title: String,
    val description: String,
    val tags: String,
    val imagePath: String,
    var likeCount: Int = 0,
    var downloadCount: Int = 0,
    var isLiked: Boolean = false,
    var isDownloaded: Boolean = false,
    val ownerId: Int = 0
)