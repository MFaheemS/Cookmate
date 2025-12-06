package com.fast.smdproject

data class User(
    val userId: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val profileImage: String?,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val uploadsCount: Int = 0
)

