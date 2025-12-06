package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONException
import java.util.concurrent.TimeUnit

class followings : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var profileImage: ImageView
    private lateinit var username: TextView
    private lateinit var lastSync: TextView
    private lateinit var refreshText: TextView
    private val followingList = ArrayList<User>()
    private var allFollowing = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followings)

        // Initialize views
        initializeViews()

        // Setup RecyclerView
        setupRecyclerView()

        // Load user profile info
        loadUserProfileInfo()

        // Load cached data first
        loadCachedFollowing()

        // Update last sync time display
        updateLastSyncDisplay()

        // Sync with server
        syncFollowing("")
    }

    private fun initializeViews() {
        searchInput = findViewById(R.id.searchInput)
        recyclerView = findViewById(R.id.recyclerViewUsers)
        profileImage = findViewById(R.id.profileImage)
        username = findViewById(R.id.username)
        lastSync = findViewById(R.id.lastSync)
        refreshText = findViewById(R.id.refreshText)

        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val profileIcon = findViewById<ImageView>(R.id.profileIcon)

        menuIcon.setOnClickListener {
            finish()
        }

        profileIcon.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        refreshText.setOnClickListener {
            syncFollowing(searchInput.text.toString())
        }

        // Search functionality
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFollowing(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        val ipAddress = getString(R.string.ipAddress)
        userAdapter = UserAdapter(this, followingList, ipAddress)
        recyclerView.adapter = userAdapter
    }

    private fun loadUserProfileInfo() {
        val db = UserDatabase(this)
        val userInfo = db.getUserInfo()

        username.text = userInfo["username"] ?: "User"

        val profileImagePath = userInfo["profile_image"] ?: ""
        if (profileImagePath.isNotEmpty() && profileImagePath != "null") {
            val ipAddress = getString(R.string.ipAddress)
            val imageUrl = "http://$ipAddress/cookMate/$profileImagePath"
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.profile_image)
                .error(R.drawable.profile_image)
                .into(profileImage)
        } else {
            profileImage.setImageResource(R.drawable.profile_image)
        }
    }

    private fun loadCachedFollowing() {
        val db = UserDatabase(this)
        allFollowing = ArrayList(db.getCachedFollowing())
        followingList.clear()
        followingList.addAll(allFollowing)
        userAdapter.notifyDataSetChanged()
    }

    private fun updateLastSyncDisplay() {
        val db = UserDatabase(this)
        val lastSyncTime = db.getLastSyncTime("following")

        if (lastSyncTime > 0) {
            val timeAgo = getTimeAgo(lastSyncTime)
            lastSync.text = "Last sync: $timeAgo"
        } else {
            lastSync.text = "Never synced"
        }
    }

    private fun getTimeAgo(time: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - time

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} minutes ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
            diff < TimeUnit.DAYS.toMillis(30) -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
            diff < TimeUnit.DAYS.toMillis(365) -> "${TimeUnit.MILLISECONDS.toDays(diff) / 30} months ago"
            else -> "${TimeUnit.MILLISECONDS.toDays(diff) / 365} years ago"
        }
    }

    private fun filterFollowing(query: String) {
        val filtered = if (query.isEmpty()) {
            allFollowing
        } else {
            allFollowing.filter {
                it.username.startsWith(query, ignoreCase = true) ||
                it.firstName.startsWith(query, ignoreCase = true) ||
                it.lastName.startsWith(query, ignoreCase = true)
            }
        }

        followingList.clear()
        followingList.addAll(filtered)
        userAdapter.notifyDataSetChanged()
    }

    private fun syncFollowing(query: String) {
        val db = UserDatabase(this)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_following.php?user_id=$userId&query="

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val jsonArray = response.getJSONArray("data")
                        val fetchedFollowing = mutableListOf<User>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val user = User(
                                userId = obj.getInt("user_id"),
                                username = obj.getString("username"),
                                firstName = obj.getString("first_name"),
                                lastName = obj.getString("last_name"),
                                profileImage = obj.optString("profile_image", "")
                            )
                            fetchedFollowing.add(user)
                        }

                        // Save to cache
                        db.saveFollowing(fetchedFollowing)

                        // Update UI
                        allFollowing = ArrayList(fetchedFollowing)
                        filterFollowing(query)
                        updateLastSyncDisplay()
                        updateResultsCount()

                        Toast.makeText(this, "Synced successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: Using cached data", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}