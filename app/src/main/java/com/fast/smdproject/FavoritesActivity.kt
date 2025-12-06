package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
    private val recipeList = ArrayList<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        val btnHome = findViewById<ImageButton>(R.id.home)
        val searchBtn = findViewById<ImageButton>(R.id.search)
        val btnUpload = findViewById<ImageButton>(R.id.upload)
        val profileBtn = findViewById<ImageButton>(R.id.profile)
        val downloadBtn = findViewById<Button>(R.id.btnDownloads)

        // Setup RecyclerView
        recyclerView = findViewById(R.id.downloadsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val ipAddress = getString(R.string.ipAddress)
        recipeAdapter = RecipeAdapter(this, recipeList, ipAddress)
        recyclerView.adapter = recipeAdapter

        // Fetch liked recipes
        fetchLikedRecipes()

        downloadBtn.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        btnHome.setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
        btnUpload.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        searchBtn.setOnClickListener { startActivity(Intent(this, SearchUserActivity::class.java)) }
        profileBtn.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
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
}