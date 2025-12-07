package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException

class ReminderActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var reminderAdapter: ReminderAdapter
    private val reminderList = mutableListOf<Reminder>()
    private val reminderMap = LinkedHashMap<Int, Reminder>() // Use map to ensure uniqueness by recipeId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)

        // Initialize views
        recyclerView = findViewById(R.id.remindersRecyclerView)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        val ipAddress = getString(R.string.ipAddress)
        reminderAdapter = ReminderAdapter(this, reminderList, ipAddress) {
            loadReminders()
        }
        recyclerView.adapter = reminderAdapter

        // Load reminders
        loadReminders()

        // Setup navigation
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Refresh reminders when returning to this screen
        loadReminders()
    }

    private fun loadReminders() {
        reminderList.clear()
        reminderMap.clear()

        val db = UserDatabase(this)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // First load from local database (works offline)
        loadFromLocalDatabase(userId, db)

        // Then try to sync with server (only when online)
        syncRemindersWithServer(userId, db)
    }

    private fun loadFromLocalDatabase(userId: Int, db: UserDatabase) {
        // Clean up expired reminders first
        db.deleteExpiredReminders(userId)

        // Load active reminders from database
        val reminders = db.getReminders(userId)

        reminderList.clear()
        reminderMap.clear()

        reminders.forEach { reminder ->
            reminderMap[reminder.recipeId] = reminder
        }

        reminderList.addAll(reminderMap.values)
        reminderList.sortBy { it.timeInMillis }
        reminderAdapter.notifyDataSetChanged()

        if (reminderList.isEmpty()) {
            Toast.makeText(this, "No upcoming reminders", Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncRemindersWithServer(userId: Int, db: UserDatabase) {
        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_reminders.php?user_id=$userId"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val jsonArray = response.getJSONArray("data")
                        val currentTime = System.currentTimeMillis()

                        // Clear existing reminders and sync from server
                        reminderMap.clear()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val recipeId = obj.getInt("recipe_id")
                            val recipeTitle = obj.getString("recipe_title")
                            val reminderTime = obj.getString("reminder_time").toLong()
                            val imagePath = obj.optString("image_path", "")

                            // Only process future reminders
                            if (reminderTime > currentTime) {
                                val reminder = Reminder(recipeId, recipeTitle, reminderTime, imagePath)
                                reminderMap[recipeId] = reminder

                                // Save to local database
                                db.saveReminder(reminder, userId)
                            }
                        }

                        // Update sync timestamp
                        db.updateRemindersSyncStatus(userId)

                        // Update UI
                        reminderList.clear()
                        reminderList.addAll(reminderMap.values)
                        reminderList.sortBy { it.timeInMillis }
                        reminderAdapter.notifyDataSetChanged()

                        if (reminderList.isEmpty()) {
                            Toast.makeText(this, "No upcoming reminders", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Server returned error, keep local data
                        Toast.makeText(this, "Using offline data", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Using offline data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                // Network error, keep local data
                Toast.makeText(this, "Offline mode - showing cached reminders", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }


    private fun setupNavigation() {
        val btnHome = findViewById<ImageView>(R.id.home)
        val searchBtn = findViewById<ImageView>(R.id.search)
        val btnUpload = findViewById<ImageView>(R.id.upload)
        val btnLibrary = findViewById<ImageView>(R.id.library)
        val profileBtn = findViewById<ImageView>(R.id.profile)

        // Set library indicator as active (current page)
        showNavIndicator(R.id.nav_lib_indicator)

        btnHome.setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
        searchBtn.setOnClickListener { startActivity(Intent(this, SearchUserActivity::class.java)) }
        btnUpload.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        btnLibrary.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        profileBtn.setOnClickListener { startActivity(Intent(this, UserProfileActivity::class.java)) }
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
