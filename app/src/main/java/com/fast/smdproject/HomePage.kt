package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException

class HomePage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = ArrayList<Recipe>()
    private var fallingLeavesAnimation: FallingLeavesAnimation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        // Process any pending actions from offline mode
        processPendingActions()

        // Check for new notifications
        checkForNotifications()

        // Set home indicator as active (current page)
        showNavIndicator(R.id.nav_home_indicator)

        // Initialize falling leaves animation
        initializeFallingLeaves()

        // Bottom Nav Setup
        val btnSearch = findViewById<ImageView>(R.id.search)
        val btnUpload = findViewById<ImageView>(R.id.upload)
        val searchBar = findViewById<EditText>(R.id.search_bar)
        val libBtn = findViewById<ImageView>(R.id.lib)
        val profileBtn = findViewById<ImageView>(R.id.profile)

        searchBar.setOnClickListener {
            com.fast.smdproject.AnimationUtils.bounce(it)
            val intent = Intent(this, SearchRecipeActivity::class.java)
            startActivity(intent)
        }

        btnSearch.setOnClickListener {
            com.fast.smdproject.AnimationUtils.bounce(it)
            startActivity(Intent(this, SearchUserActivity::class.java))
        }
        btnUpload.setOnClickListener {
            com.fast.smdproject.AnimationUtils.bounce(it)
            startActivity(Intent(this, UploadRecipe::class.java))
        }
        libBtn.setOnClickListener {
            com.fast.smdproject.AnimationUtils.bounce(it)
            startActivity(Intent(this, DownloadsActivity::class.java))
        }
        profileBtn.setOnClickListener {
            com.fast.smdproject.AnimationUtils.bounce(it)
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        // RecyclerView Setup
        recyclerView = findViewById(R.id.recycler_view_recipes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Animate Recommended text periodically
        animateRecommendedText()

        // Fetch Data
        fetchRecipes()
    }

    private fun animateRecommendedText() {
        val recommendedTitle = findViewById<android.widget.TextView>(R.id.recommended_title)

        // Create periodic animation (every 3 seconds for more frequent animation)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val animationRunnable = object : Runnable {
            override fun run() {
                // Bounce animation
                recommendedTitle.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(200)
                    .withEndAction {
                        recommendedTitle.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200)
                            .start()
                    }
                    .start()

                // Shake animation (slight rotation)
                recommendedTitle.animate()
                    .rotation(5f)
                    .setDuration(100)
                    .withEndAction {
                        recommendedTitle.animate()
                            .rotation(-5f)
                            .setDuration(100)
                            .withEndAction {
                                recommendedTitle.animate()
                                    .rotation(3f)
                                    .setDuration(100)
                                    .withEndAction {
                                        recommendedTitle.animate()
                                            .rotation(-3f)
                                            .setDuration(100)
                                            .withEndAction {
                                                recommendedTitle.animate()
                                                    .rotation(0f)
                                                    .setDuration(100)
                                                    .start()
                                            }
                                            .start()
                                    }
                                    .start()
                            }
                            .start()
                    }
                    .start()

                // Schedule next animation (every 3 seconds - more frequent)
                handler.postDelayed(this, 3000)
            }
        }

        // Start the periodic animation after a short delay
        handler.postDelayed(animationRunnable, 1500)
    }

    private fun fetchRecipes() {
        val db = UserDatabase(this)
        val userId = db.getCurrentUserId()

        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_all_recipes.php?user_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val jsonArray = response.getJSONArray("data")
                        recipeList.clear()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)

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
                                downloadCount = obj.optInt("download_count", 0),
                                isLiked = obj.optBoolean("is_liked", false),
                                isDownloaded = obj.optBoolean("is_downloaded", false),
                                ownerId = obj.optInt("user_id", 0)
                            )
                            recipeList.add(recipe)
                        }

                        // Attach Adapter
                        recipeAdapter = RecipeAdapter(this, recipeList, ipAddress)
                        recyclerView.adapter = recipeAdapter
                    } else {
                        Toast.makeText(this, "No recipes found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Json Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun processPendingActions() {
        val actionQueue = ActionQueueService(this)
        actionQueue.processPendingActions {
            // Actions processed, optionally refresh data
        }
    }

    private fun checkForNotifications() {
        val db = UserDatabase(this)
        val userId = db.getCurrentUserId()

        if (userId == 0) return

        val prefs = getSharedPreferences("Notifications", MODE_PRIVATE)
        val lastCheck = prefs.getString("last_notification_check", "")

        val ipAddress = getString(R.string.ipAddress)
        val url = if (lastCheck.isNullOrEmpty()) {
            "http://$ipAddress/cookMate/get_notifications.php?user_id=$userId"
        } else {
            "http://$ipAddress/cookMate/get_notifications.php?user_id=$userId&last_check=$lastCheck"
        }

        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val notifications = response.getJSONArray("notifications")

                        for (i in 0 until notifications.length()) {
                            val notif = notifications.getJSONObject(i)
                            val type = notif.getString("type")

                            when (type) {
                                "new_follower" -> {
                                    val username = notif.getString("username")
                                    NotificationHelper.showFollowerNotification(this, username)
                                }
                                "new_recipe" -> {
                                    val recipeTitle = notif.getString("recipe_title")
                                    val authorUsername = notif.getString("author_username")
                                    val recipeId = notif.getInt("recipe_id")
                                    NotificationHelper.showNewRecipeNotification(
                                        this,
                                        authorUsername,
                                        recipeTitle,
                                        recipeId
                                    )
                                }
                            }
                        }

                        // Update last check timestamp
                        val currentTime = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date())
                        prefs.edit().putString("last_notification_check", currentTime).apply()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Silently fail - notifications are not critical
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun showNavIndicator(indicatorId: Int) {
        // Hide all indicators first with fade out
        val indicators = listOf(
            R.id.nav_home_indicator,
            R.id.nav_search_indicator,
            R.id.nav_upload_indicator,
            R.id.nav_lib_indicator,
            R.id.nav_profile_indicator
        )

        indicators.forEach { id ->
            findViewById<android.view.View>(id)?.let { indicator ->
                if (id == indicatorId) {
                    // Fade in the active indicator
                    indicator.visibility = android.view.View.VISIBLE
                    indicator.alpha = 0f
                    indicator.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                } else {
                    // Fade out inactive indicators
                    indicator.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            indicator.visibility = android.view.View.GONE
                        }
                        .start()
                }
            }
        }
    }

    private fun initializeFallingLeaves() {
        val header = findViewById<android.view.View>(R.id.header)
        val leavesContainer = findViewById<android.view.ViewGroup>(R.id.falling_leaves_container)

        // Post to ensure header is measured
        header.post {
            val headerHeight = header.height
            fallingLeavesAnimation = FallingLeavesAnimation(this, leavesContainer, headerHeight)
            fallingLeavesAnimation?.start()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchRecipes()

        // Restart falling leaves animation
        fallingLeavesAnimation?.start()
    }

    override fun onPause() {
        super.onPause()

        // Stop falling leaves animation when activity is paused
        fallingLeavesAnimation?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up falling leaves animation
        fallingLeavesAnimation?.stop()
        fallingLeavesAnimation = null
    }
}