package com.fast.smdproject

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONArray
import org.json.JSONObject

class RecipeDetail : AppCompatActivity() {

    // UI Components
    private lateinit var imageGallery: ViewPager2
    private lateinit var imageCounter: TextView
    private lateinit var txtTitle: TextView
    private lateinit var txtDesc: TextView
    private lateinit var imgUser: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var tagsContainer: LinearLayout
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnFollow : Button
    private lateinit var txtFavCount : TextView
    private lateinit var txtDownloadCount : TextView
    private lateinit var iconLike: ImageView
    private lateinit var iconDownload: ImageView

    private var db: UserDatabase? = null
    private var currentRecipeId: Int = -1
    private var currentUserId: Int = 0
    private var recipeOwnerId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        // Init Views
        imageGallery = findViewById(R.id.image_gallery)
        imageCounter = findViewById(R.id.image_counter)
        txtTitle = findViewById(R.id.recipe_title)
        txtDesc = findViewById(R.id.recipe_desc)
        imgUser = findViewById(R.id.user_avatar)
        txtUsername = findViewById(R.id.username)
        tagsContainer = findViewById(R.id.tags_container)
        ingredientsContainer = findViewById(R.id.ingredients_list_container)
        stepsContainer = findViewById(R.id.steps_list_container)
        btnBack = findViewById(R.id.btn_back)
        btnFollow = findViewById(R.id.btn_follow)
        txtFavCount = findViewById(R.id.favCounter)
        txtDownloadCount = findViewById(R.id.downloadCounter)
        iconLike = findViewById(R.id.icon_like_detail)
        iconDownload = findViewById(R.id.icon_download_detail)

        btnBack.setOnClickListener { finish() }

        db = UserDatabase(this)
        currentUserId = db?.getCurrentUserId() ?: 0

        // Setup ViewPager2 for smooth transitions
        setupImageGallery()

        val recipeId = intent.getIntExtra("RECIPE_ID", -1)
        currentRecipeId = recipeId

        if (recipeId != -1) {
            fetchRecipeDetails(recipeId)
        } else {
            Toast.makeText(this, "Error: Invalid Recipe ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupImageGallery() {
        // Enable smooth page transitions with fading effect
        imageGallery.setPageTransformer { page, position ->
            page.apply {
                val absPos = Math.abs(position)
                alpha = 1f - absPos
                scaleX = 1f - (absPos * 0.1f)
                scaleY = 1f - (absPos * 0.1f)
            }
        }
    }

    private fun fetchRecipeDetails(id: Int) {
        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_recipe_detail.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val data = json.getJSONObject("data")
                        populateUI(data, ipAddress)
                    } else {
                        Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["recipe_id"] = id.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun populateUI(data: JSONObject, ipAddress: String) {
        // 1. Basic Info
        txtTitle.text = data.getString("title")
        txtDesc.text = data.getString("description")

        val favCount = data.getInt("favorites_count")
        val downloadCount = data.getInt("downloads_count")
        val isLiked = data.optBoolean("is_liked", false)
        val isDownloaded = data.optBoolean("is_downloaded", false)
        recipeOwnerId = data.optInt("user_id", 0)

        txtFavCount.text = favCount.toString()
        txtDownloadCount.text = downloadCount.toString()

        // Set like icon based on status
        iconLike.setImageResource(if (isLiked) R.drawable.ic_like_fill else R.drawable.ic_like)

        // Set download icon based on status
        iconDownload.setImageResource(if (isDownloaded) R.drawable.ic_downloaded else R.drawable.ic_download)

        // Check if user is viewing their own recipe
        val isOwnRecipe = (db != null && db!!.getUsername() == data.getString("username"))

        if(isOwnRecipe){
            txtUsername.text = "You"
            btnFollow.visibility = View.GONE
            // Hide like/download for own recipes
            iconLike.visibility = View.GONE
            iconDownload.visibility = View.GONE
        }
        else{
            txtUsername.text = data.getString("username")
            btnFollow.visibility = View.VISIBLE
            iconLike.visibility = View.VISIBLE
            iconDownload.visibility = View.VISIBLE

            // Setup click listeners for like and download
            iconLike.setOnClickListener {
                toggleLike()
            }

            iconDownload.setOnClickListener {
                toggleDownload()
            }
        }

        // 2. Load Images Gallery
        val imagesString = data.getString("images")
        val imagesList = mutableListOf<String>()

        try {
            // Try to parse as JSON array (new format)
            val imagesArray = JSONArray(imagesString)
            for (i in 0 until imagesArray.length()) {
                imagesList.add(imagesArray.getString(i))
            }
        } catch (e: Exception) {
            // Fallback: treat as single image path (old format)
            if (imagesString.isNotEmpty()) {
                imagesList.add(imagesString)
            }
        }

        if (imagesList.isNotEmpty()) {
            // Setup image gallery
            val adapter = ImageGalleryAdapter(imagesList, ipAddress)
            imageGallery.adapter = adapter

            // Set initial position to middle of infinite list
            val middlePosition = Int.MAX_VALUE / 2
            val startPosition = middlePosition - (middlePosition % imagesList.size)
            imageGallery.setCurrentItem(startPosition, false)

            // Keep counter hidden
            imageCounter.visibility = View.GONE
        }

        val userImg = data.getString("profile_image")
        if (!userImg.isNullOrEmpty()) {
            val fullUrl = "http://$ipAddress/cookMate/$userImg"
            Glide.with(this)
                .load(fullUrl)
                .circleCrop()
                .placeholder(R.drawable.default_pfp)
                .error(R.drawable.default_pfp)
                .into(imgUser)
        } else {
            Glide.with(this)
                .load(R.drawable.default_pfp)
                .circleCrop()
                .into(imgUser)
        }

        // 4. Populate Tags
        val tagsString = data.getString("tags")
        val tagsList = tagsString.split(",")
        tagsContainer.removeAllViews()
        for (tag in tagsList) {
            val textView = TextView(this)
            textView.text = tag.trim()
            textView.setTextColor(resources.getColor(R.color.white))
            textView.textSize = 12f
            textView.setPadding(20, 10, 20, 10)
            tagsContainer.addView(textView)
        }

        // 5. Populate Ingredients (JSON Array)
        val ingredientsJson = data.getString("ingredients")
        val ingArray = JSONArray(ingredientsJson)
        ingredientsContainer.removeAllViews()

        for (i in 0 until ingArray.length()) {
            val obj = ingArray.getJSONObject(i)
            // Inflate our helper row
            val view = layoutInflater.inflate(R.layout.item_ingredient_display, null)
            val txtName = view.findViewById<TextView>(R.id.txt_ing_name)
            val txtQty = view.findViewById<TextView>(R.id.txt_ing_qty)

            txtName.text = obj.getString("name")
            txtQty.text = obj.getString("qty")

            ingredientsContainer.addView(view)
        }

        // 6. Populate Steps (JSON Array)
        val stepsJson = data.getString("steps")
        val stepsArray = JSONArray(stepsJson)
        stepsContainer.removeAllViews()

        for (i in 0 until stepsArray.length()) {
            val stepDesc = stepsArray.getString(i)

            val view = layoutInflater.inflate(R.layout.item_step_display, null)
            val txtNum = view.findViewById<TextView>(R.id.txt_step_number)
            val txtDesc = view.findViewById<TextView>(R.id.txt_step_desc)

            txtNum.text = "Step ${i + 1}"
            txtDesc.text = stepDesc

            stepsContainer.addView(view)
        }
    }

    private fun toggleLike() {
        // Check current like status from icon
        val isCurrentlyLiked = iconLike.drawable.constantState ==
            resources.getDrawable(R.drawable.ic_like_fill, null)?.constantState

        val ipAddress = getString(R.string.ipAddress)
        val url = if (isCurrentlyLiked) {
            "http://$ipAddress/cookMate/unlike_recipe.php"
        } else {
            "http://$ipAddress/cookMate/like_recipe.php"
        }

        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        // Update icon
                        iconLike.setImageResource(
                            if (isCurrentlyLiked) R.drawable.ic_like else R.drawable.ic_like_fill
                        )

                        // Update count
                        val currentCount = txtFavCount.text.toString().toIntOrNull() ?: 0
                        val newCount = if (isCurrentlyLiked) currentCount - 1 else currentCount + 1
                        txtFavCount.text = newCount.toString()

                        val message = if (isCurrentlyLiked) "Removed from favorites" else "Added to favorites"
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = currentUserId.toString()
                params["recipe_id"] = currentRecipeId.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun toggleDownload() {
        // Check current download status from icon
        val isCurrentlyDownloaded = iconDownload.drawable.constantState ==
            resources.getDrawable(R.drawable.ic_downloaded, null)?.constantState

        val ipAddress = getString(R.string.ipAddress)
        val url = if (isCurrentlyDownloaded) {
            "http://$ipAddress/cookMate/remove_download.php"
        } else {
            "http://$ipAddress/cookMate/download_recipe.php"
        }

        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        // Update icon
                        iconDownload.setImageResource(
                            if (isCurrentlyDownloaded) R.drawable.ic_download else R.drawable.ic_downloaded
                        )

                        // Update count
                        val currentCount = txtDownloadCount.text.toString().toIntOrNull() ?: 0
                        val newCount = if (isCurrentlyDownloaded) currentCount - 1 else currentCount + 1
                        txtDownloadCount.text = newCount.toString()

                        val message = if (isCurrentlyDownloaded) "Removed from downloads" else "Added to downloads"
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = currentUserId.toString()
                params["recipe_id"] = currentRecipeId.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}