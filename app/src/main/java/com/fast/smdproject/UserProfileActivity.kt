package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONArray
import org.json.JSONException

class UserProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var tvFollowersCount: TextView
    private lateinit var tvUploadsCount: TextView
    private lateinit var recyclerViewRecipes: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var recipeAdapter: ProfileRecipeAdapter
    private val recipeList = ArrayList<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Initialize views
        initializeViews()

        // Setup navigation
        setupNavigation()

        // Setup RecyclerView
        setupRecyclerView()

        // Fetch user profile data
        fetchUserProfile()
    }

    private fun initializeViews() {
        ivProfile = findViewById(R.id.ivProfile)
        tvUsername = findViewById(R.id.tvUsername)
        tvFollowingCount = findViewById(R.id.tvFollowingCount)
        tvFollowersCount = findViewById(R.id.tvFollowersCount)
        tvUploadsCount = findViewById(R.id.tvUploadsCount)
        recyclerViewRecipes = findViewById(R.id.recyclerViewRecipes)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        // Click listeners for followers/following
        tvFollowersCount.setOnClickListener {
            val intent = Intent(this, followers::class.java)
            startActivity(intent)
        }

        tvFollowingCount.setOnClickListener {
            val intent = Intent(this, followings::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        recyclerViewRecipes.layoutManager = GridLayoutManager(this, 2)
        val ipAddress = getString(R.string.ipAddress)
        recipeAdapter = ProfileRecipeAdapter(this, recipeList, ipAddress) {
            // Refresh profile when a recipe is deleted
            fetchUserProfile()
        }
        recyclerViewRecipes.adapter = recipeAdapter
    }

    private fun setupNavigation() {
        val editBtn = findViewById<ImageView>(R.id.ivEdit)
        val btnHome = findViewById<ImageButton>(R.id.home)
        val searchBtn = findViewById<ImageButton>(R.id.search)
        val btnUpload = findViewById<ImageButton>(R.id.upload)
        val libBtn = findViewById<ImageButton>(R.id.lib)
        val settings = findViewById<ImageView>(R.id.ivSettings)

        settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        libBtn.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        btnHome.setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
        btnUpload.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        searchBtn.setOnClickListener { startActivity(Intent(this, SearchUserActivity::class.java)) }

        editBtn.setOnClickListener {
            startActivity(Intent(this, EditUserProfileActivity::class.java))
        }
    }

    private fun fetchUserProfile() {
        // Get current user ID from database
        val db = UserDatabase(this)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Load from cache first (works offline)
        loadProfileFromCache(db, userId)

        // Then sync with server (when online)
        syncProfileWithServer(db, userId)
    }

    private fun loadProfileFromCache(db: UserDatabase, userId: Int) {
        // Load user info
        val userInfo = db.getUserInfo()
        if (userInfo.isNotEmpty()) {
            tvUsername.text = userInfo["username"] ?: ""

            val profileImage = userInfo["profile_image"] ?: ""
            if (profileImage.isNotEmpty() && profileImage != "null") {
                val ipAddress = getString(R.string.ipAddress)
                val imageUrl = "http://$ipAddress/cookMate/$profileImage"
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.profile_image)
                    .error(R.drawable.profile_image)
                    .into(ivProfile)
            }
        }

        // Load cached counts
        val counts = db.getUserCounts()
        tvFollowersCount.text = counts.first.toString()
        tvFollowingCount.text = counts.second.toString()
        tvUploadsCount.text = counts.third.toString()

        // Load cached uploads
        val cachedUploads = db.getUserUploads(userId)
        if (cachedUploads.isNotEmpty()) {
            recipeList.clear()
            tvEmptyState.visibility = View.GONE
            recyclerViewRecipes.visibility = View.VISIBLE

            for (uploadMap in cachedUploads) {
                val recipe = Recipe(
                    recipeId = uploadMap["recipe_id"]?.toIntOrNull() ?: 0,
                    title = uploadMap["title"] ?: "",
                    description = uploadMap["description"] ?: "",
                    tags = uploadMap["tags"] ?: "",
                    imagePath = try {
                        val imagesString = uploadMap["images"] ?: "[]"
                        val imagesArray = JSONArray(imagesString)
                        if (imagesArray.length() > 0) imagesArray.getString(0) else ""
                    } catch (e: Exception) {
                        uploadMap["images"] ?: ""
                    },
                    likeCount = uploadMap["like_count"]?.toIntOrNull() ?: 0,
                    downloadCount = uploadMap["download_count"]?.toIntOrNull() ?: 0
                )
                recipeList.add(recipe)
            }
            recipeAdapter.notifyDataSetChanged()
        } else {
            tvEmptyState.visibility = View.VISIBLE
            recyclerViewRecipes.visibility = View.GONE
        }
    }

    private fun syncProfileWithServer(db: UserDatabase, userId: Int) {
        val ipAddress = getString(R.string.ipAddress)
        // Add viewer_id to ensure own profile always loads
        val url = "http://$ipAddress/cookMate/get_user_profile.php?user_id=$userId&viewer_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val userData = response.getJSONObject("data")

                        // Set user information
                        val username = userData.getString("username")
                        val profileImage = userData.optString("profile_image", "")
                        val followersCount = userData.getInt("followers_count")
                        val followingCount = userData.getInt("following_count")
                        val uploadsCount = userData.getInt("uploads_count")

                        // Update cached counts
                        db.updateUserCounts(followersCount, followingCount, uploadsCount)

                        tvUsername.text = username
                        tvFollowersCount.text = followersCount.toString()
                        tvFollowingCount.text = followingCount.toString()
                        tvUploadsCount.text = uploadsCount.toString()

                        // Load profile image
                        if (profileImage.isNotEmpty() && profileImage != "null") {
                            val imageUrl = "http://$ipAddress/cookMate/$profileImage"
                            Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.profile_image)
                                .error(R.drawable.profile_image)
                                .into(ivProfile)
                        }

                        // Load recipes and cache them
                        val recipesArray = userData.getJSONArray("recipes")

                        // Clear old cache and save new uploads
                        db.clearUserUploads(userId)
                        recipeList.clear()

                        if (recipesArray.length() == 0) {
                            // Show empty state
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerViewRecipes.visibility = View.GONE
                        } else {
                            // Show recipes
                            tvEmptyState.visibility = View.GONE
                            recyclerViewRecipes.visibility = View.VISIBLE

                            for (i in 0 until recipesArray.length()) {
                                val obj = recipesArray.getJSONObject(i)

                                // Parse images - extract first image as cover
                                val imagesString = obj.getString("images")
                                val coverImage = try {
                                    val imagesArray = JSONArray(imagesString)
                                    if (imagesArray.length() > 0) {
                                        imagesArray.getString(0)
                                    } else {
                                        ""
                                    }
                                } catch (e: Exception) {
                                    // Fallback for old single image format
                                    imagesString
                                }

                                val recipe = Recipe(
                                    recipeId = obj.getInt("recipe_id"),
                                    title = obj.getString("title"),
                                    description = obj.getString("description"),
                                    tags = obj.getString("tags"),
                                    imagePath = coverImage,
                                    likeCount = obj.optInt("like_count", 0),
                                    downloadCount = obj.optInt("download_count", 0)
                                )
                                recipeList.add(recipe)

                                // Cache upload to local database
                                db.saveUserUpload(
                                    recipeId = obj.getInt("recipe_id"),
                                    userId = userId,
                                    title = obj.getString("title"),
                                    description = obj.getString("description"),
                                    ingredients = obj.optString("ingredients", "[]"),
                                    steps = obj.optString("steps", "[]"),
                                    tags = obj.getString("tags"),
                                    images = imagesString,
                                    likeCount = obj.optInt("like_count", 0),
                                    downloadCount = obj.optInt("download_count", 0),
                                    createdAt = obj.optString("created_at", "")
                                )
                            }

                            recipeAdapter.notifyDataSetChanged()
                        }

                    } else {
                        Toast.makeText(this, "Using cached profile data", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Using cached profile data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Offline mode - showing cached uploads", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data when returning to this activity
        fetchUserProfile()
    }
}