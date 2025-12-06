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
import org.json.JSONException

class SearchUserActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var resultsCount: TextView
    private lateinit var refreshText: TextView
    private lateinit var menuIcon: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var recyclerViewUsers: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_user)

        // Initialize views
        initializeViews()

        // Set up listeners
        setupListeners()

        // Set up bottom navigation
        setupBottomNavigation()

        // Setup RecyclerView
        setupRecyclerView()
    }

    private fun initializeViews() {
        searchInput = findViewById(R.id.searchInput)
        resultsCount = findViewById(R.id.resultsCount)
        refreshText = findViewById(R.id.refreshText)
        menuIcon = findViewById(R.id.menuIcon)
        profileIcon = findViewById(R.id.profileIcon)
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers)
    }

    private fun setupRecyclerView() {
        recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        val ipAddress = getString(R.string.ipAddress)
        userAdapter = UserAdapter(this, userList, ipAddress)
        recyclerViewUsers.adapter = userAdapter
    }

    private fun setupListeners() {
        // Back button listener
        menuIcon.setOnClickListener {
            finish()
        }

        // Profile icon listener
        profileIcon.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        // Search input listener
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Perform search
                performSearch(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Refresh button listener
        refreshText.setOnClickListener {
            refreshUserList()
        }
    }

    private fun performSearch(query: String) {
        val ipAddress = getString(R.string.ipAddress)
        val trimmedQuery = query.trim()

        // Get current user ID
        val db = UserDatabase(this)
        val currentUserId = db.getCurrentUserId()

        // If query is empty, clear results
        if (trimmedQuery.isEmpty()) {
            userList.clear()
            userAdapter.notifyDataSetChanged()
            resultsCount.text = "0 results"
            return
        }

        val url = "http://$ipAddress/cookMate/search_users.php?query=$trimmedQuery&current_user_id=$currentUserId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val jsonArray = response.getJSONArray("data")
                        userList.clear()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)

                            val user = User(
                                userId = obj.getInt("user_id"),
                                username = obj.getString("username"),
                                firstName = obj.getString("first_name"),
                                lastName = obj.getString("last_name"),
                                profileImage = obj.optString("profile_image", "")
                            )
                            userList.add(user)
                        }

                        userAdapter.notifyDataSetChanged()
                        resultsCount.text = "${userList.size} results"
                    } else {
                        userList.clear()
                        userAdapter.notifyDataSetChanged()
                        resultsCount.text = "0 results"
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

    private fun refreshUserList() {
        // Re-perform the current search
        performSearch(searchInput.text.toString())
    }

    private fun setupBottomNavigation() {
        val homeBtn = findViewById<ImageView>(R.id.homeNav)
        val searchBtn = findViewById<ImageView>(R.id.searchNav)
        val uploadBtn = findViewById<ImageView>(R.id.uploadNav)
        val libBtn = findViewById<ImageView>(R.id.libNav)
        val profileBtn = findViewById<ImageView>(R.id.profileNav)

        homeBtn.setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
        searchBtn.setOnClickListener { /* Already on search user activity */ }
        uploadBtn.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        libBtn.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        profileBtn.setOnClickListener { startActivity(Intent(this, UserProfileActivity::class.java)) }
    }
}

