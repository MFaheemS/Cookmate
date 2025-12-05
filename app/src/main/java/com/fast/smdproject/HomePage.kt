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
import org.json.JSONException

class HomePage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = ArrayList<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        // Bottom Nav Setup
        val btnSearch = findViewById<ImageView>(R.id.search)
        val btnUpload = findViewById<ImageView>(R.id.upload)
        val searchBar = findViewById<EditText>(R.id.search_bar)
        val libBtn = findViewById<ImageView>(R.id.lib)
        val profileBtn = findViewById<ImageView>(R.id.profile)

        searchBar.setOnClickListener {

            val intent = Intent(this, search::class.java)
            startActivity(intent)
        }

        btnSearch.setOnClickListener { startActivity(Intent(this, search::class.java)) }
        btnUpload.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        libBtn.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        profileBtn.setOnClickListener { startActivity(Intent(this, UserProfileActivity::class.java)) }

        // RecyclerView Setup
        recyclerView = findViewById(R.id.recycler_view_recipes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Fetch Data
        fetchRecipes()
    }

    private fun fetchRecipes() {
        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_all_recipes.php"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val jsonArray = response.getJSONArray("data")
                        recipeList.clear()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)

                            val recipe = Recipe(
                                recipeId = obj.getInt("recipe_id"),
                                title = obj.getString("title"),
                                description = obj.getString("description"),
                                tags = obj.getString("tags"),
                                imagePath = obj.getString("images") // This gets "media_uploads/..."
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

    override fun onResume() {
        super.onResume()

        fetchRecipes()
    }
}