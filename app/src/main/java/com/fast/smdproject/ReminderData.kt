package com.fast.smdproject

data class Reminder(
    val recipeId: Int,
    val recipeTitle: String,
    val timeInMillis: Long,
    val imagePath: String = ""
)

