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
import org.json.JSONArray
import org.json.JSONException

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var searchBar: EditText
    private lateinit var favoriteBtn: Button
    private lateinit var downloadBtn: Button
    private val recipeList = ArrayList<Recipe>()
    private val allRecipes = ArrayList<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        val btnHome = findViewById<ImageButton>(R.id.home)
        val searchBtn = findViewById<ImageButton>(R.id.search)
        val btnUpload = findViewById<ImageButton>(R.id.upload)
        val profileBtn = findViewById<ImageButton>(R.id.profile)
        downloadBtn = findViewById(R.id.btnDownloads)
        favoriteBtn = findViewById(R.id.btnFavorites)
        searchBar = findViewById(R.id.searchDownloads)

        // Setup RecyclerView
        recyclerView = findViewById(R.id.downloadsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val ipAddress = getString(R.string.ipAddress)
        recipeAdapter = RecipeAdapter(this, recipeList, ipAddress)
        recyclerView.adapter = recipeAdapter

        // Set active state for favorites button
        setActiveButton(favoriteBtn, true)
        setActiveButton(downloadBtn, false)

        // Set library indicator as active (current page)
        showNavIndicator(R.id.nav_lib_indicator)

        // Setup search functionality
        setupSearch()

        // Fetch liked recipes
        fetchLikedRecipes()

        downloadBtn.setOnClickListener {
            AnimationUtils.buttonPressEffect(it) {
                startActivity(Intent(this, DownloadsActivity::class.java))
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
        recipeAdapter.notifyDataSetChanged()
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

    private fun fetchLikedRecipes() {
        val db = UserDatabase(this)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_liked_recipes.php?user_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val jsonArray = response.getJSONArray("data")
                        recipeList.clear()
                        allRecipes.clear()

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
                                imagesString
                            }

                            val recipe = Recipe(
                                recipeId = obj.getInt("recipe_id"),
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
                            recipeList.add(recipe)
                            allRecipes.add(recipe)
                        }

                        recipeAdapter.notifyDataSetChanged()

                        if (recipeList.isEmpty()) {
                            Toast.makeText(this, "No favorites yet", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Failed to load favorites", Toast.LENGTH_SHORT).show()
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

    private fun showNavIndicator(indicatorId: Int) {
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
                    indicator.visibility = android.view.View.VISIBLE
                    indicator.alpha = 0f
                    indicator.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                } else {
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
}