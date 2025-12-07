package com.fast.smdproject

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONException

class DownloadsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var downloadRecipeAdapter: DownloadRecipeAdapter
    private lateinit var searchBar: EditText
    private lateinit var favoriteBtn: Button
    private lateinit var downloadBtn: Button
    private val recipeList = ArrayList<Recipe>()
    private val allRecipes = ArrayList<Recipe>()
    private val downloadTimes = HashMap<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        val btnHome = findViewById<ImageButton>(R.id.home)
        val searchBtn = findViewById<ImageButton>(R.id.search)
        val btnUpload = findViewById<ImageButton>(R.id.upload)
        val profileBtn = findViewById<ImageButton>(R.id.profile)
        favoriteBtn = findViewById(R.id.btnFavorites)
        downloadBtn = findViewById(R.id.btnDownloads)
        searchBar = findViewById(R.id.searchDownloads)
        val fabReminders = findViewById<FloatingActionButton>(R.id.fabReminders)

        // Setup RecyclerView
        recyclerView = findViewById<RecyclerView>(R.id.downloadsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val ipAddress = getString(R.string.ipAddress)
        downloadRecipeAdapter = DownloadRecipeAdapter(this, recipeList, ipAddress, downloadTimes)
        recyclerView.adapter = downloadRecipeAdapter

        // Set active state for downloads button
        setActiveButton(downloadBtn, true)
        setActiveButton(favoriteBtn, false)

        // Setup search functionality
        setupSearch()

        // Fetch downloaded recipes
        fetchDownloadedRecipes()

        // Navigation listeners with animations
        favoriteBtn.setOnClickListener {
            AnimationUtils.buttonPressEffect(it) {
                startActivity(Intent(this, FavoritesActivity::class.java))
                overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit)
            }
        }
        btnHome.setOnClickListener {
            AnimationUtils.bounce(it)
            startActivity(Intent(this, HomePage::class.java))
        }
        btnUpload.setOnClickListener {
            AnimationUtils.bounce(it)
            startActivity(Intent(this, UploadRecipe::class.java))
        }
        searchBtn.setOnClickListener {
            AnimationUtils.bounce(it)
            startActivity(Intent(this, SearchUserActivity::class.java))
        }
        profileBtn.setOnClickListener {
            AnimationUtils.bounce(it)
            startActivity(Intent(this, UserProfileActivity::class.java))
        }
        fabReminders.setOnClickListener {
            AnimationUtils.bounce(it)
            startActivity(Intent(this, ReminderActivity::class.java))
        }
    }

    private fun setupSearch() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterRecipes(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterRecipes(query: String) {
        val filteredList = if (query.isEmpty()) {
            ArrayList(allRecipes)
        } else {
            allRecipes.filter { recipe ->
                // Check if any word in title starts with the query
                recipe.title.split(" ").any { it.startsWith(query, ignoreCase = true) } ||
                // Check if any word in description starts with the query
                recipe.description.split(" ").any { it.startsWith(query, ignoreCase = true) } ||
                // Check if any tag starts with the query
                recipe.tags.split(",", " ").any { it.trim().startsWith(query, ignoreCase = true) }
            } as ArrayList<Recipe>
        }

        recipeList.clear()
        recipeList.addAll(filteredList)
        downloadRecipeAdapter.notifyDataSetChanged()
    }

    private fun setActiveButton(button: Button, isActive: Boolean) {
        if (isActive) {
            button.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .start()
            button.setBackgroundResource(R.drawable.button_green_rounded)
            button.setTextColor(Color.WHITE)
        } else {
            button.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start()
            button.setBackgroundResource(R.drawable.button_rounded_gray)
            button.setTextColor(Color.parseColor("#666666"))
        }
    }

    private fun fetchDownloadedRecipes() {
        val db = UserDatabase(this)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // First, load from local SQLite (works offline)
        loadFromLocalDatabase(userId, db)

        // Then try to sync with server (only when online)
        syncWithServer(userId, db)
    }

    private fun loadFromLocalDatabase(userId: Int, db: UserDatabase) {
        val (recipes, times) = db.getDownloadedRecipes(userId)

        recipeList.clear()
        allRecipes.clear()
        downloadTimes.clear()

        recipeList.addAll(recipes)
        allRecipes.addAll(recipes)
        downloadTimes.putAll(times)

        downloadRecipeAdapter.notifyDataSetChanged()

        if (recipeList.isEmpty()) {
            Toast.makeText(this, "No downloads yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncWithServer(userId: Int, db: UserDatabase) {
        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_downloaded_recipes.php?user_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val jsonArray = response.getJSONArray("data")

                        // Clear and sync with server data
                        db.clearDownloadedRecipes(userId)
                        recipeList.clear()
                        allRecipes.clear()
                        downloadTimes.clear()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val recipeId = obj.getInt("recipe_id")

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
                                imagesString
                            }

                            val recipe = Recipe(
                                recipeId = recipeId,
                                title = obj.getString("title"),
                                description = obj.getString("description"),
                                tags = obj.getString("tags"),
                                imagePath = coverImage,
                                likeCount = obj.getInt("like_count"),
                                downloadCount = obj.getInt("download_count"),
                                isLiked = obj.getBoolean("is_liked"),
                                isDownloaded = obj.getBoolean("is_downloaded"),
                                ownerId = obj.optInt("user_id", 0)
                            )

                            val downloadedAt = obj.getString("downloaded_at")

                            // Save to local database
                            db.saveDownloadedRecipe(recipe, userId, downloadedAt)

                            recipeList.add(recipe)
                            allRecipes.add(recipe)
                            downloadTimes[recipeId] = downloadedAt
                        }

                        // Update sync timestamp
                        db.updateDownloadedRecipesSyncStatus(userId)

                        downloadRecipeAdapter.notifyDataSetChanged()

                        if (recipeList.isEmpty()) {
                            Toast.makeText(this, "No downloads yet", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Server error, keep local data
                        Toast.makeText(this, "Using offline data", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Using offline data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                // Network error, keep local data
                Toast.makeText(this, "Offline mode - showing cached downloads", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}