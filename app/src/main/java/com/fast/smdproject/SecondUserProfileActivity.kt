package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class SecondUserProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvFollowersCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var tvUploadsCount: TextView
    private lateinit var ivSettings: ImageView
    private lateinit var ivSave: TextView
    private lateinit var recyclerViewRecipes: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var followButton: RelativeLayout
    private lateinit var followButtonText: TextView
    private lateinit var recipeAdapter: SecondUserRecipeAdapter
    private val recipeList = ArrayList<Recipe>()

    private var userId: Int = 0
    private var username: String = ""
    private var isFollowing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.second_user_profile)

        // Get user ID from intent
        userId = intent.getIntExtra("user_id", 0)
        username = intent.getStringExtra("username") ?: ""

        if (userId == 0) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        initializeViews()

        // Setup RecyclerView
        setupRecyclerView()

        // Setup navigation
        setupNavigation()

        // Fetch user profile data
        fetchUserProfile()

        // Check follow status
        checkFollowStatus()
    }

    private fun initializeViews() {
        ivProfile = findViewById(R.id.ivProfile)
        tvUsername = findViewById(R.id.tvUsername)
        tvFollowersCount = findViewById(R.id.tvFollowersCount)
        tvFollowingCount = findViewById(R.id.tvFollowingCount)
        tvUploadsCount = findViewById(R.id.tvUploadsCount)
        ivSettings = findViewById(R.id.ivSettings)
        ivSave = findViewById(R.id.ivSave)
        recyclerViewRecipes = findViewById(R.id.recyclerViewRecipes)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        followButton = findViewById(R.id.followButton)
        followButtonText = findViewById(R.id.followButtonText)

        // Back button
        ivSettings.setOnClickListener {
            finish()
        }

        // Follow button click
        followButton.setOnClickListener {
            toggleFollow()
        }
    }

    private fun setupRecyclerView() {
        recyclerViewRecipes.layoutManager = GridLayoutManager(this, 2)
        val ipAddress = getString(R.string.ipAddress)

        // Get current logged in user ID
        val db = UserDatabase(this)
        val currentUserId = db.getCurrentUserId()

        recipeAdapter = SecondUserRecipeAdapter(this, recipeList, ipAddress, currentUserId)
        recyclerViewRecipes.adapter = recipeAdapter
    }

    private fun setupNavigation() {
        // Bottom navigation - it's a LinearLayout with ImageButtons
        val bottomNav = findViewById<android.widget.LinearLayout>(R.id.bottomNav)

        // Get all ImageButtons
        val navButtons = mutableListOf<android.widget.ImageButton>()
        for (i in 0 until bottomNav.childCount) {
            val child = bottomNav.getChildAt(i)
            if (child is android.widget.ImageButton) {
                navButtons.add(child)
            }
        }

        // Setup click listeners (order: home, search, upload, lib, profile)
        if (navButtons.size >= 5) {
            navButtons[0].setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
            navButtons[1].setOnClickListener { startActivity(Intent(this, SearchUserActivity::class.java)) }
            navButtons[2].setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
            navButtons[3].setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
            navButtons[4].setOnClickListener { startActivity(Intent(this, UserProfileActivity::class.java)) }
        }
    }

    private fun fetchUserProfile() {
        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_user_profile.php?user_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val userData = response.getJSONObject("data")

                        // Set user information
                        val usernameText = userData.getString("username")
                        val profileImage = userData.optString("profile_image", "")
                        val followersCount = userData.getInt("followers_count")
                        val followingCount = userData.getInt("following_count")
                        val uploadsCount = userData.getInt("uploads_count")

                        tvUsername.text = usernameText
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


                        // Load recipes
                        val recipesArray = userData.getJSONArray("recipes")
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
                                    imagePath = coverImage
                                )
                                recipeList.add(recipe)
                            }

                            recipeAdapter.notifyDataSetChanged()
                        }

                    } else {
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun checkFollowStatus() {
        val db = UserDatabase(this)
        val currentUserId = db.getCurrentUserId()

        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/check_follow_status.php?follower_id=$currentUserId&following_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        isFollowing = response.getBoolean("is_following")
                        updateFollowButton()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Silently fail, default to not following
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun toggleFollow() {
        val db = UserDatabase(this)
        val currentUserId = db.getCurrentUserId()

        if (currentUserId == 0) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val ipAddress = getString(R.string.ipAddress)
        val endpoint = if (isFollowing) "unfollow_user.php" else "follow_user.php"
        val url = "http://$ipAddress/cookMate/$endpoint"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        isFollowing = !isFollowing
                        updateFollowButton()

                        // Update follower count
                        val currentCount = tvFollowersCount.text.toString().toIntOrNull() ?: 0
                        val newCount = if (isFollowing) currentCount + 1 else currentCount - 1
                        tvFollowersCount.text = newCount.toString()

                        Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["follower_id"] = currentUserId.toString()
                params["following_id"] = userId.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun updateFollowButton() {
        if (isFollowing) {
            followButtonText.text = "Unfollow"
            followButton.setBackgroundResource(R.drawable.light_green_bg)
        } else {
            followButtonText.text = "Follow $username"
            followButton.setBackgroundResource(R.drawable.light_green_bg)
        }
    }
}

