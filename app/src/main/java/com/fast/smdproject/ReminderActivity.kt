package com.fast.smdproject

import android.content.Context
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

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val recipeId = obj.getInt("recipe_id")
                            val recipeTitle = obj.getString("recipe_title")
                            val reminderTime = obj.getString("reminder_time").toLong()
                            val imagePath = obj.optString("image_path", "")

                            // Only show future reminders
                            if (reminderTime > currentTime) {
                                // Use map to ensure only one reminder per recipeId
                                reminderMap[recipeId] = Reminder(recipeId, recipeTitle, reminderTime, imagePath)

                                // Also save to local SharedPreferences for offline access
                                saveToLocalCache(recipeId, recipeTitle, reminderTime, imagePath)
                            }
                        }

                        // Convert map to list and sort by time
                        reminderList.clear()
                        reminderList.addAll(reminderMap.values)
                        reminderList.sortBy { it.timeInMillis }
                        reminderAdapter.notifyDataSetChanged()

                        if (reminderList.isEmpty()) {
                            Toast.makeText(this, "No upcoming reminders", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val message = response.optString("message", "Failed to load reminders")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        // Fallback to local cache
                        loadFromLocalCache()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error parsing response: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Fallback to local SharedPreferences if network fails
                    loadFromLocalCache()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}. Loading from cache...", Toast.LENGTH_SHORT).show()
                // Fallback to local SharedPreferences
                loadFromLocalCache()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun saveToLocalCache(recipeId: Int, title: String, time: Long, imagePath: String) {
        val sharedPrefs = getSharedPreferences("Reminders", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putLong("reminder_$recipeId", time)
        editor.putString("reminder_title_$recipeId", title)
        editor.putString("reminder_image_$recipeId", imagePath)
        editor.apply()
    }

    private fun loadFromLocalCache() {
        reminderList.clear()
        reminderMap.clear()

        val sharedPrefs = getSharedPreferences("Reminders", Context.MODE_PRIVATE)
        val allPrefs = sharedPrefs.all

        // Find all reminder entries
        val reminderIds = mutableSetOf<Int>()
        for (key in allPrefs.keys) {
            if (key.startsWith("reminder_") && !key.contains("title") && !key.contains("image")) {
                val recipeId = key.removePrefix("reminder_").toIntOrNull()
                if (recipeId != null) {
                    reminderIds.add(recipeId)
                }
            }
        }

        // Load each reminder using map to ensure uniqueness
        val currentTime = System.currentTimeMillis()
        for (recipeId in reminderIds) {
            val timeInMillis = sharedPrefs.getLong("reminder_$recipeId", 0L)

            // Only show future reminders
            if (timeInMillis > currentTime) {
                val title = sharedPrefs.getString("reminder_title_$recipeId", "Unknown Recipe") ?: "Unknown Recipe"
                val imagePath = sharedPrefs.getString("reminder_image_$recipeId", "") ?: ""

                // Use map to ensure only one reminder per recipeId
                reminderMap[recipeId] = Reminder(recipeId, title, timeInMillis, imagePath)
            } else {
                // Clean up expired reminders
                val editor = sharedPrefs.edit()
                editor.remove("reminder_$recipeId")
                editor.remove("reminder_title_$recipeId")
                editor.remove("reminder_image_$recipeId")
                editor.apply()
            }
        }

        // Convert map to list and sort by time
        reminderList.clear()
        reminderList.addAll(reminderMap.values)
        reminderList.sortBy { it.timeInMillis }
        reminderAdapter.notifyDataSetChanged()

        if (reminderList.isEmpty()) {
            Toast.makeText(this, "No reminders in cache", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigation() {
        val btnHome = findViewById<ImageView>(R.id.home)
        val searchBtn = findViewById<ImageView>(R.id.search)
        val btnUpload = findViewById<ImageView>(R.id.upload)
        val btnLibrary = findViewById<ImageView>(R.id.library)
        val profileBtn = findViewById<ImageView>(R.id.profile)

        btnHome.setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
        searchBtn.setOnClickListener { startActivity(Intent(this, SearchUserActivity::class.java)) }
        btnUpload.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        btnLibrary.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        profileBtn.setOnClickListener { startActivity(Intent(this, UserProfileActivity::class.java)) }
    }
}

