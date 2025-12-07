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
    private var jungleLeavesAnimation: JungleLeavesAnimation? = null

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

        // Check if opened from shake gesture (for jungle leaves animation)
        val fromShake = intent.getBooleanExtra("FROM_SHAKE", false)

        if (recipeId != -1) {
            fetchRecipeDetails(recipeId)

            // Trigger jungle leaves animation if opened from shake
            if (fromShake) {
                triggerJungleLeavesAnimation()
            }
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
        // Try loading from cache first (works offline)
        val cachedRecipe = db?.getRecipeDetail(id)
        if (cachedRecipe != null) {
            populateUIFromCache(cachedRecipe)
        }

        // Then try to fetch from server (sync when online)
        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_recipe_detail.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val data = json.getJSONObject("data")

                        // Save to cache for offline access
                        cacheRecipeDetails(data)

                        populateUI(data, ipAddress)
                    } else {
                        if (cachedRecipe == null) {
                            Toast.makeText(this, json.getString("message"), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Showing cached data", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (cachedRecipe == null) {
                        Toast.makeText(this, "Failed to load recipe", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Offline mode - showing cached data", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            { error ->
                if (cachedRecipe == null) {
                    Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Offline mode - showing cached recipe", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["recipe_id"] = id.toString()
                params["user_id"] = currentUserId.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun cacheRecipeDetails(data: JSONObject) {
        db?.saveRecipeDetail(
            recipeId = data.getInt("recipe_id"),
            title = data.getString("title"),
            description = data.getString("description"),
            ingredients = data.getString("ingredients"),
            steps = data.getString("steps"),
            tags = data.getString("tags"),
            images = data.getString("images"),
            likeCount = data.optInt("favorites_count", data.optInt("like_count", 0)),
            downloadCount = data.optInt("downloads_count", data.optInt("download_count", 0)),
            isLiked = data.optBoolean("is_liked", false),
            isDownloaded = data.optBoolean("is_downloaded", false),
            ownerId = data.optInt("user_id", 0),
            ownerUsername = data.getString("username"),
            ownerProfileImage = data.optString("profile_image", "")
        )
    }

    private fun populateUIFromCache(cachedRecipe: HashMap<String, String>) {
        txtTitle.text = cachedRecipe["title"]
        txtDesc.text = cachedRecipe["description"]

        val favCount = cachedRecipe["like_count"]?.toIntOrNull() ?: 0
        val downloadCount = cachedRecipe["download_count"]?.toIntOrNull() ?: 0
        val isLiked = cachedRecipe["is_liked"] == "1"
        val isDownloaded = cachedRecipe["is_downloaded"] == "1"
        recipeOwnerId = cachedRecipe["owner_id"]?.toIntOrNull() ?: 0

        txtFavCount.text = favCount.toString()
        txtDownloadCount.text = downloadCount.toString()

        iconLike.setImageResource(if (isLiked) R.drawable.ic_like_fill else R.drawable.ic_like)
        iconDownload.setImageResource(if (isDownloaded) R.drawable.ic_downloaded else R.drawable.ic_download)

        val ownerUsername = cachedRecipe["owner_username"] ?: ""
        val isOwnRecipe = (db != null && db!!.getUsername() == ownerUsername)

        if(isOwnRecipe){
            txtUsername.text = "You"
            btnFollow.visibility = View.GONE
            iconLike.alpha = 0.3f
            iconDownload.alpha = 0.3f
            iconLike.isEnabled = false
            iconDownload.isEnabled = false
        } else {
            txtUsername.text = ownerUsername
            btnFollow.visibility = View.VISIBLE
            iconLike.alpha = 1.0f
            iconDownload.alpha = 1.0f
            iconLike.isEnabled = true
            iconDownload.isEnabled = true
        }

        // Load images
        val imagesString = cachedRecipe["images"] ?: "[]"
        loadImagesFromCache(imagesString)

        // Load tags
        val tagsString = cachedRecipe["tags"] ?: ""
        loadTagsFromCache(tagsString)

        // Load ingredients
        val ingredientsString = cachedRecipe["ingredients"] ?: ""
        loadIngredientsFromCache(ingredientsString)

        // Load steps
        val stepsString = cachedRecipe["steps"] ?: ""
        loadStepsFromCache(stepsString)

        // Load profile image - try to load from network with proper fallback
        val profileImage = cachedRecipe["owner_profile_image"] ?: ""
        if (profileImage.isNotEmpty() && profileImage != "null") {
            val ipAddress = getString(R.string.ipAddress)
            Glide.with(this)
                .load("http://$ipAddress/cookMate/$profileImage")
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

        setupClickListeners(isOwnRecipe)
    }

    private fun loadImagesFromCache(imagesString: String) {
        try {
            val imagesArray = JSONArray(imagesString)
            val imagesList = mutableListOf<String>()
            for (i in 0 until imagesArray.length()) {
                imagesList.add(imagesArray.getString(i))
            }

            if (imagesList.isNotEmpty()) {
                val ipAddress = getString(R.string.ipAddress)
                val adapter = ImageGalleryAdapter(imagesList, ipAddress)
                imageGallery.adapter = adapter

                // Set initial position to middle of infinite list
                val middlePosition = Int.MAX_VALUE / 2
                val startPosition = middlePosition - (middlePosition % imagesList.size)
                imageGallery.setCurrentItem(startPosition, false)

                imageCounter.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadTagsFromCache(tagsString: String) {
        tagsContainer.removeAllViews()
        val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        for (tag in tags) {
            val textView = TextView(this)
            textView.text = tag.trim()
            textView.setTextColor(resources.getColor(R.color.white, null))
            textView.textSize = 12f
            textView.setPadding(20, 10, 20, 10)
            tagsContainer.addView(textView)
        }
    }

    private fun loadIngredientsFromCache(ingredientsString: String) {
        ingredientsContainer.removeAllViews()
        try {
            val ingredientsArray = JSONArray(ingredientsString)
            for (i in 0 until ingredientsArray.length()) {
                val obj = ingredientsArray.getJSONObject(i)
                val view = layoutInflater.inflate(R.layout.item_ingredient_display, ingredientsContainer, false)
                val txtName = view.findViewById<TextView>(R.id.txt_ing_name)
                val txtQty = view.findViewById<TextView>(R.id.txt_ing_qty)

                txtName.text = obj.getString("name")
                txtQty.text = obj.getString("qty")

                ingredientsContainer.addView(view)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadStepsFromCache(stepsString: String) {
        stepsContainer.removeAllViews()
        try {
            val stepsArray = JSONArray(stepsString)
            for (i in 0 until stepsArray.length()) {
                val stepDesc = stepsArray.getString(i)

                val view = layoutInflater.inflate(R.layout.item_step_display, stepsContainer, false)
                val txtNum = view.findViewById<TextView>(R.id.txt_step_number)
                val txtDesc = view.findViewById<TextView>(R.id.txt_step_desc)

                txtNum.text = "Step ${i + 1}"
                txtDesc.text = stepDesc

                stepsContainer.addView(view)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupClickListeners(isOwnRecipe: Boolean) {
        if (isOwnRecipe) {
            iconLike.setOnClickListener {
                Toast.makeText(this, "You cannot like your own recipe", Toast.LENGTH_SHORT).show()
            }
            iconDownload.setOnClickListener {
                Toast.makeText(this, "You cannot download your own recipe", Toast.LENGTH_SHORT).show()
            }
        } else {
            iconLike.setOnClickListener { toggleLike() }
            iconDownload.setOnClickListener { toggleDownload() }
        }
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
            // Grey out like/download for own recipes (disable but show counts)
            iconLike.alpha = 0.3f
            iconDownload.alpha = 0.3f
            iconLike.isEnabled = false
            iconDownload.isEnabled = false
        }
        else{
            txtUsername.text = data.getString("username")
            btnFollow.visibility = View.VISIBLE
            iconLike.alpha = 1.0f
            iconDownload.alpha = 1.0f
            iconLike.isEnabled = true
            iconDownload.isEnabled = true

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

        // Add bounce animation
        com.fast.smdproject.AnimationUtils.bounce(iconLike)

        // Update UI optimistically (immediately)
        iconLike.setImageResource(
            if (isCurrentlyLiked) R.drawable.ic_like else R.drawable.ic_like_fill
        )

        val currentCount = txtFavCount.text.toString().toIntOrNull() ?: 0
        val newCount = if (isCurrentlyLiked) currentCount - 1 else currentCount + 1
        txtFavCount.text = newCount.toString()

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
                        // Update the cached recipe details with new counts and state
                        val db = UserDatabase(this)
                        val cachedRecipe = db.getRecipeDetail(currentRecipeId)
                        if (cachedRecipe != null) {
                            // Update the cached data with new values
                            db.saveRecipeDetail(
                                recipeId = currentRecipeId,
                                title = cachedRecipe["title"] ?: "",
                                description = cachedRecipe["description"] ?: "",
                                ingredients = cachedRecipe["ingredients"] ?: "",
                                steps = cachedRecipe["steps"] ?: "",
                                tags = cachedRecipe["tags"] ?: "",
                                images = cachedRecipe["images"] ?: "",
                                likeCount = json.optInt("like_count", newCount),
                                downloadCount = cachedRecipe["download_count"]?.toIntOrNull() ?: 0,
                                isLiked = !isCurrentlyLiked,
                                isDownloaded = cachedRecipe["is_downloaded"] == "1",
                                ownerId = cachedRecipe["owner_id"]?.toIntOrNull() ?: 0,
                                ownerUsername = cachedRecipe["owner_username"] ?: "",
                                ownerProfileImage = cachedRecipe["owner_profile_image"] ?: ""
                            )
                        }

                        val message = if (isCurrentlyLiked) "Removed from favorites" else "Added to favorites"
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    } else {
                        // Revert UI on server error
                        iconLike.setImageResource(
                            if (isCurrentlyLiked) R.drawable.ic_like_fill else R.drawable.ic_like
                        )
                        txtFavCount.text = currentCount.toString()
                        Toast.makeText(this, "Action failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Revert UI on error
                    iconLike.setImageResource(
                        if (isCurrentlyLiked) R.drawable.ic_like_fill else R.drawable.ic_like
                    )
                    txtFavCount.text = currentCount.toString()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                // Queue action for later if network fails
                val db = UserDatabase(this)
                val actionType = if (isCurrentlyLiked) "unlike" else "like"
                db.queueAction(currentUserId, actionType, currentRecipeId, "recipe")

                // Update cached recipe with optimistic state
                val cachedRecipe = db.getRecipeDetail(currentRecipeId)
                if (cachedRecipe != null) {
                    db.saveRecipeDetail(
                        recipeId = currentRecipeId,
                        title = cachedRecipe["title"] ?: "",
                        description = cachedRecipe["description"] ?: "",
                        ingredients = cachedRecipe["ingredients"] ?: "",
                        steps = cachedRecipe["steps"] ?: "",
                        tags = cachedRecipe["tags"] ?: "",
                        images = cachedRecipe["images"] ?: "",
                        likeCount = newCount,
                        downloadCount = cachedRecipe["download_count"]?.toIntOrNull() ?: 0,
                        isLiked = !isCurrentlyLiked,
                        isDownloaded = cachedRecipe["is_downloaded"] == "1",
                        ownerId = cachedRecipe["owner_id"]?.toIntOrNull() ?: 0,
                        ownerUsername = cachedRecipe["owner_username"] ?: "",
                        ownerProfileImage = cachedRecipe["owner_profile_image"] ?: ""
                    )
                }

                Toast.makeText(this, "Action queued - will sync when online", Toast.LENGTH_SHORT).show()
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

        val db = UserDatabase(this)

        // Add bounce animation
        com.fast.smdproject.AnimationUtils.bounce(iconDownload)

        // Update UI optimistically (immediately)
        iconDownload.setImageResource(
            if (isCurrentlyDownloaded) R.drawable.ic_download else R.drawable.ic_downloaded
        )

        val currentCount = txtDownloadCount.text.toString().toIntOrNull() ?: 0
        val newCount = if (isCurrentlyDownloaded) currentCount - 1 else currentCount + 1
        txtDownloadCount.text = newCount.toString()

        // Update local database immediately
        if (!isCurrentlyDownloaded) {
            fetchAndSaveRecipeToLocal(currentRecipeId)
        } else {
            db.deleteDownloadedRecipe(currentRecipeId, currentUserId)
            // Don't delete recipe detail cache, just update the download state
        }

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
                        // Update the cached recipe details with new counts and state
                        val cachedRecipe = db.getRecipeDetail(currentRecipeId)
                        if (cachedRecipe != null) {
                            db.saveRecipeDetail(
                                recipeId = currentRecipeId,
                                title = cachedRecipe["title"] ?: "",
                                description = cachedRecipe["description"] ?: "",
                                ingredients = cachedRecipe["ingredients"] ?: "",
                                steps = cachedRecipe["steps"] ?: "",
                                tags = cachedRecipe["tags"] ?: "",
                                images = cachedRecipe["images"] ?: "",
                                likeCount = cachedRecipe["like_count"]?.toIntOrNull() ?: 0,
                                downloadCount = json.optInt("download_count", newCount),
                                isLiked = cachedRecipe["is_liked"] == "1",
                                isDownloaded = !isCurrentlyDownloaded,
                                ownerId = cachedRecipe["owner_id"]?.toIntOrNull() ?: 0,
                                ownerUsername = cachedRecipe["owner_username"] ?: "",
                                ownerProfileImage = cachedRecipe["owner_profile_image"] ?: ""
                            )
                        }

                        val message = if (isCurrentlyDownloaded) "Removed from downloads" else "Added to downloads"
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    } else {
                        // Revert UI on server error
                        iconDownload.setImageResource(
                            if (isCurrentlyDownloaded) R.drawable.ic_downloaded else R.drawable.ic_download
                        )
                        txtDownloadCount.text = currentCount.toString()
                        Toast.makeText(this, "Action failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Revert UI on error
                    iconDownload.setImageResource(
                        if (isCurrentlyDownloaded) R.drawable.ic_downloaded else R.drawable.ic_download
                    )
                    txtDownloadCount.text = currentCount.toString()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                // Queue action for later if network fails
                val actionType = if (isCurrentlyDownloaded) "undownload" else "download"
                db.queueAction(currentUserId, actionType, currentRecipeId, "recipe")

                // Update cached recipe with optimistic state
                val cachedRecipe = db.getRecipeDetail(currentRecipeId)
                if (cachedRecipe != null) {
                    db.saveRecipeDetail(
                        recipeId = currentRecipeId,
                        title = cachedRecipe["title"] ?: "",
                        description = cachedRecipe["description"] ?: "",
                        ingredients = cachedRecipe["ingredients"] ?: "",
                        steps = cachedRecipe["steps"] ?: "",
                        tags = cachedRecipe["tags"] ?: "",
                        images = cachedRecipe["images"] ?: "",
                        likeCount = cachedRecipe["like_count"]?.toIntOrNull() ?: 0,
                        downloadCount = newCount,
                        isLiked = cachedRecipe["is_liked"] == "1",
                        isDownloaded = !isCurrentlyDownloaded,
                        ownerId = cachedRecipe["owner_id"]?.toIntOrNull() ?: 0,
                        ownerUsername = cachedRecipe["owner_username"] ?: "",
                        ownerProfileImage = cachedRecipe["owner_profile_image"] ?: ""
                    )
                }

                Toast.makeText(this, "Action queued - will sync when online", Toast.LENGTH_SHORT).show()
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

    private fun fetchAndSaveRecipeToLocal(recipeId: Int) {
        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_recipe_detail.php"

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val data = json.getJSONObject("data")

                        // Parse images
                        val imagesString = data.getString("images")
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
                            recipeId = data.getInt("recipe_id"),
                            title = data.getString("title"),
                            description = data.getString("description"),
                            tags = data.getString("tags"),
                            imagePath = coverImage,
                            likeCount = data.optInt("favorites_count", data.optInt("like_count", 0)),
                            downloadCount = data.optInt("downloads_count", data.optInt("download_count", 0)),
                            isLiked = data.optBoolean("is_liked", false),
                            isDownloaded = true,
                            ownerId = data.getInt("user_id")
                        )

                        val db = UserDatabase(this)
                        val downloadedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        db.saveDownloadedRecipe(recipe, currentUserId, downloadedAt)

                        // Also cache full recipe details for offline viewing
                        db.saveRecipeDetail(
                            recipeId = data.getInt("recipe_id"),
                            title = data.getString("title"),
                            description = data.getString("description"),
                            ingredients = data.getString("ingredients"),
                            steps = data.getString("steps"),
                            tags = data.getString("tags"),
                            images = imagesString,
                            likeCount = data.optInt("favorites_count", data.optInt("like_count", 0)),
                            downloadCount = data.optInt("downloads_count", data.optInt("download_count", 0)),
                            isLiked = data.optBoolean("is_liked", false),
                            isDownloaded = true,
                            ownerId = data.getInt("user_id"),
                            ownerUsername = data.getString("username"),
                            ownerProfileImage = data.optString("profile_image", "")
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["recipe_id"] = recipeId.toString()
                params["user_id"] = currentUserId.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    /**
     * Trigger jungle leaves animation - called when recipe is opened from shake gesture
     */
    private fun triggerJungleLeavesAnimation() {
        val leavesContainer = findViewById<android.widget.FrameLayout>(R.id.jungle_leaves_container)

        // Wait a brief moment for the activity transition to complete
        leavesContainer.postDelayed({
            jungleLeavesAnimation = JungleLeavesAnimation(this, leavesContainer)
            // Trigger burst with 25 leaves for a dramatic jungle effect
            jungleLeavesAnimation?.triggerBurst(25)

            android.util.Log.d("JungleLeaves", "Jungle leaves animation triggered!")
        }, 300) // Short delay for smooth transition
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any remaining leaves
        jungleLeavesAnimation?.cleanup()
    }
}