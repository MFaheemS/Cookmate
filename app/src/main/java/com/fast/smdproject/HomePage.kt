package com.fast.smdproject

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import kotlin.math.sqrt

class HomePage : AppCompatActivity(), SensorEventListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = ArrayList<Recipe>()
    private var fallingLeavesAnimation: FallingLeavesAnimation? = null

    // Shake detection variables
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private val SHAKE_THRESHOLD = 12f // Sensitivity threshold (lowered for better detection)
    private val SHAKE_COOLDOWN = 2000L // 2 seconds between shakes

    // Variables to track previous acceleration values
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastUpdate: Long = 0

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

        // Initialize shake detection for random recipe feature
        initializeShakeDetection()

        // Animate the shake hint card to attract attention
        animateShakeHintCard()

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

    // ============== SHAKE DETECTION FEATURE ==============

    private fun initializeShakeDetection() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Toast.makeText(this, "Device doesn't support shake detection", Toast.LENGTH_SHORT).show()
            android.util.Log.e("ShakeDetection", "Accelerometer not available!")
        } else {
            android.util.Log.d("ShakeDetection", "Accelerometer initialized successfully")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()

            // Only check every 100ms to avoid too frequent updates
            if (currentTime - lastUpdate < 100) {
                return
            }

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate the change in acceleration (delta)
            val deltaX = Math.abs(lastX - x)
            val deltaY = Math.abs(lastY - y)
            val deltaZ = Math.abs(lastZ - z)

            val acceleration = deltaX + deltaY + deltaZ

            // Debug logging
            if (acceleration > 5) { // Log any significant movement
                android.util.Log.d("ShakeDetection", "Acceleration: $acceleration, Threshold: $SHAKE_THRESHOLD")
            }

            // Check if shake threshold is exceeded
            if (acceleration > SHAKE_THRESHOLD && currentTime - lastShakeTime > SHAKE_COOLDOWN) {
                lastShakeTime = currentTime
                onShakeDetected()
            }

            // Update last values
            lastX = x
            lastY = y
            lastZ = z
            lastUpdate = currentTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for shake detection
    }

    private fun onShakeDetected() {
        // Debug: Show that shake was detected
        android.util.Log.d("ShakeDetection", "Shake detected!")

        // Vibrate for haptic feedback
        vibrateDevice()

        // Flash the shake hint card
        flashShakeHintCard()

        // Check if we have recipes to choose from
        if (recipeList.isEmpty()) {
            showShakeToast("No recipes available! 📭", false)
            return
        }

        // Select a random recipe
        val randomRecipe = recipeList.random()

        // Show animated toast
        showShakeToast("🎲 Random recipe selected!", true)

        // Animate the RecyclerView with a shake effect
        animateRecyclerViewShake()

        // Open the random recipe after a short delay (for effect)
        recyclerView.postDelayed({
            val intent = Intent(this, RecipeDetail::class.java).apply {
                putExtra("RECIPE_ID", randomRecipe.recipeId)
                putExtra("FROM_SHAKE", true) // Flag for jungle leaves animation
            }
            android.util.Log.d("ShakeDetection", "Opening recipe with ID: ${randomRecipe.recipeId}")
            startActivity(intent)

            // Add slide-in transition animation
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }, 600)
    }

    private fun flashShakeHintCard() {
        val shakeHintCard = findViewById<androidx.cardview.widget.CardView>(R.id.shake_hint_card) ?: return

        // Flash effect - quick scale burst
        shakeHintCard.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(100)
            .withEndAction {
                shakeHintCard.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .setInterpolator(android.view.animation.BounceInterpolator())
                    .start()
            }
            .start()

        // Color flash using alpha animation
        shakeHintCard.animate()
            .alpha(0.6f)
            .setDuration(100)
            .withEndAction {
                shakeHintCard.animate()
                    .alpha(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun vibrateDevice() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create a fun vibration pattern: short-pause-short-pause-long
            val timings = longArrayOf(0, 100, 50, 100, 50, 200)
            val amplitudes = intArrayOf(0, 100, 0, 100, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 50, 100, 50, 200), -1)
        }
    }

    private fun animateRecyclerViewShake() {
        // Shake the entire RecyclerView with rotation
        recyclerView.animate()
            .rotation(5f)
            .setDuration(80)
            .withEndAction {
                recyclerView.animate()
                    .rotation(-5f)
                    .setDuration(80)
                    .withEndAction {
                        recyclerView.animate()
                            .rotation(3f)
                            .setDuration(80)
                            .withEndAction {
                                recyclerView.animate()
                                    .rotation(-3f)
                                    .setDuration(80)
                                    .withEndAction {
                                        recyclerView.animate()
                                            .rotation(0f)
                                            .setDuration(80)
                                            .start()
                                    }
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()

        // Also add a scale animation for emphasis
        recyclerView.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(150)
            .withEndAction {
                recyclerView.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(150)
                    .withEndAction {
                        recyclerView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun showShakeToast(message: String, isSuccess: Boolean) {
        // Create custom toast with animation
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.custom_shake_toast, findViewById(R.id.homepage), false)

        val emojiText = layout.findViewById<android.widget.TextView>(R.id.toast_emoji)
        val messageText = layout.findViewById<android.widget.TextView>(R.id.toast_message)

        emojiText.text = if (isSuccess) "🎲" else "📭"
        messageText.text = message

        // Create and position the toast
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout

        // Animate the toast entrance
        layout.alpha = 0f
        layout.scaleX = 0.5f
        layout.scaleY = 0.5f

        toast.show()

        // Animate in with bounce effect
        layout.animate()
            .alpha(1f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
    }

    private fun animateShakeHintCard() {
        val shakeHintCard = findViewById<android.view.View>(R.id.shake_hint_card) ?: return

        // Create a smooth glowing animation that repeats
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val animationRunnable = object : Runnable {
            override fun run() {
                // Glow bright (fade to full brightness)
                shakeHintCard.animate()
                    .alpha(1.0f)
                    .setDuration(1000)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .withEndAction {
                        // Dim (fade to lower brightness)
                        shakeHintCard.animate()
                            .alpha(0.5f)
                            .setDuration(1000)
                            .setInterpolator(android.view.animation.AccelerateInterpolator())
                            .start()
                    }
                    .start()

                // Repeat every 2 seconds (1 second bright + 1 second dim)
                handler.postDelayed(this, 2000)
            }
        }

        // Set initial alpha and start the animation after a short delay
        shakeHintCard.alpha = 0.5f
        handler.postDelayed(animationRunnable, 500)
    }

    // ============== END SHAKE DETECTION FEATURE ==============

    override fun onResume() {
        super.onResume()
        fetchRecipes()

        // Restart falling leaves animation
        fallingLeavesAnimation?.start()

        // Register shake detection sensor
        accelerometer?.let {
            val registered = sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            android.util.Log.d("ShakeDetection", "Sensor registered in onResume: $registered")
        } ?: run {
            android.util.Log.e("ShakeDetection", "Accelerometer is null, cannot register!")
        }
    }

    override fun onPause() {
        super.onPause()

        // Stop falling leaves animation when activity is paused
        fallingLeavesAnimation?.stop()

        // Unregister shake detection sensor to save battery
        sensorManager?.unregisterListener(this)
        android.util.Log.d("ShakeDetection", "Sensor unregistered in onPause")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up falling leaves animation
        fallingLeavesAnimation?.stop()
        fallingLeavesAnimation = null
    }
}