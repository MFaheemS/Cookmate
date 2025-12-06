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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONException

class DownloadsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var downloadRecipeAdapter: DownloadRecipeAdapter
    private val recipeList = ArrayList<Recipe>()
    private val downloadTimes = HashMap<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        val btnHome = findViewById<ImageButton>(R.id.home)
        val searchBtn = findViewById<ImageButton>(R.id.search)
        val btnUpload = findViewById<ImageButton>(R.id.upload)
        val profileBtn = findViewById<ImageButton>(R.id.profile)
        val favoriteBtn = findViewById<Button>(R.id.btnFavorites)
        val fabReminders = findViewById<FloatingActionButton>(R.id.fabReminders)

        // Setup RecyclerView
        recyclerView = findViewById<RecyclerView>(R.id.downloadsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val ipAddress = getString(R.string.ipAddress)
        downloadRecipeAdapter = DownloadRecipeAdapter(this, recipeList, ipAddress, downloadTimes)
        recyclerView.adapter = downloadRecipeAdapter

        // Fetch downloaded recipes
        fetchDownloadedRecipes()

        // Navigation listeners
        favoriteBtn.setOnClickListener { startActivity(Intent(this, FavoritesActivity::class.java)) }
        btnHome.setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
        btnUpload.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        searchBtn.setOnClickListener { startActivity(Intent(this, SearchUserActivity::class.java)) }
        profileBtn.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }
        fabReminders.setOnClickListener {
            startActivity(Intent(this, ReminderActivity::class.java))
        }
    }

    private fun fetchDownloadedRecipes() {
        val db = UserDatabase(this)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_downloaded_recipes.php?user_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val jsonArray = response.getJSONArray("data")
                        recipeList.clear()
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
                            recipeList.add(recipe)
                            downloadTimes[recipeId] = obj.getString("downloaded_at")
                        }

                        downloadRecipeAdapter.notifyDataSetChanged()

                        if (recipeList.isEmpty()) {
                            Toast.makeText(this, "No downloads yet", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Failed to load downloads", Toast.LENGTH_SHORT).show()
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