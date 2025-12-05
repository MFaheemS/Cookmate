package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class search : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var btnBack: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val searchList = ArrayList<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)


        val btnHome = findViewById<ImageView>(R.id.home)
        val btnUpload = findViewById<ImageView>(R.id.upload)
        val libBtn = findViewById<ImageView>(R.id.lib)
        val profileBtn = findViewById<ImageView>(R.id.profile)



        btnHome.setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
        btnUpload.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        libBtn.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        profileBtn.setOnClickListener { startActivity(Intent(this, UserProfileActivity::class.java)) }



        searchInput = findViewById(R.id.search_input)
        btnBack = findViewById(R.id.btn_back)
        recyclerView = findViewById(R.id.recycler_search_results)


        recyclerView.layoutManager = LinearLayoutManager(this)
        val ipAddress = getString(R.string.ipAddress)

        recipeAdapter = RecipeAdapter(this, searchList, ipAddress)
        recyclerView.adapter = recipeAdapter


        btnBack.setOnClickListener {
            finish()
        }


        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                performSearch(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performSearch(query: String) {
        val ipAddress = getString(R.string.ipAddress)

        val url = "http://$ipAddress/cookMate/search_recipes.php?query=$query"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val jsonArray = response.getJSONArray("data")
                        searchList.clear()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)

                            val recipe = Recipe(
                                recipeId = obj.getInt("recipe_id"),
                                title = obj.getString("title"),
                                description = obj.getString("description"),
                                tags = obj.getString("tags"),
                                imagePath = obj.getString("images")
                            )
                            searchList.add(recipe)
                        }


                        recipeAdapter.notifyDataSetChanged()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->

                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}