package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var db: UserDatabase
    private lateinit var switchPrivateProfile: SwitchCompat
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchRecipeNotifications: SwitchCompat
    private lateinit var switchLogoutOnClose: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        db = UserDatabase(this)

        // Initialize views
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<TextView>(R.id.btnSave)
        val logOutBtn = findViewById<MaterialButton>(R.id.btnLogout)

        switchPrivateProfile = findViewById(R.id.switchPrivateProfile)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchRecipeNotifications = findViewById(R.id.switchRecipeNotifications)
        switchLogoutOnClose = findViewById(R.id.switchLogoutOnClose)

        // Load current settings
        loadSettings()

        // Back button
        btnBack.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                finish()
            }
        }

        // Save button
        btnSave.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                saveSettings()
            }
        }

        // Logout button
        logOutBtn.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                performLogout()
            }
        }

        // Handle notification dependencies
        switchNotifications.setOnCheckedChangeListener { buttonView, isChecked ->
            // Add ripple animation
            com.fast.smdproject.AnimationUtils.rippleToggle(buttonView, isChecked)

            // If all notifications disabled, also disable recipe notifications
            if (!isChecked) {
                switchRecipeNotifications.isChecked = false
                switchRecipeNotifications.isEnabled = false
            } else {
                switchRecipeNotifications.isEnabled = true
            }
        }

        // Add animations to other switches
        switchPrivateProfile.setOnCheckedChangeListener { buttonView, isChecked ->
            com.fast.smdproject.AnimationUtils.rippleToggle(buttonView, isChecked)
        }

        switchRecipeNotifications.setOnCheckedChangeListener { buttonView, isChecked ->
            com.fast.smdproject.AnimationUtils.rippleToggle(buttonView, isChecked)
        }

        switchLogoutOnClose.setOnCheckedChangeListener { buttonView, isChecked ->
            com.fast.smdproject.AnimationUtils.rippleToggle(buttonView, isChecked)
        }
    }

    private fun loadSettings() {
        // Load settings from local database
        switchPrivateProfile.isChecked = db.isPrivateProfile()
        switchNotifications.isChecked = db.allowNotifications()
        switchRecipeNotifications.isChecked = db.allowRecipeNotifications()
        switchLogoutOnClose.isChecked = db.logoutOnClose()

        // Set recipe notifications enabled state based on all notifications
        switchRecipeNotifications.isEnabled = switchNotifications.isChecked
    }

    private fun saveSettings() {
        val isPrivate = switchPrivateProfile.isChecked
        val allowNotifications = switchNotifications.isChecked
        val allowRecipeNotifications = switchRecipeNotifications.isChecked
        val logoutOnClose = switchLogoutOnClose.isChecked

        // Save to local database
        db.updatePrivateProfile(isPrivate)
        db.updateAllowNotifications(allowNotifications)
        db.updateAllowRecipeNotifications(allowRecipeNotifications)
        db.updateLogoutOnClose(logoutOnClose)

        // Sync with server
        syncSettingsWithServer(isPrivate)

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun syncSettingsWithServer(isPrivate: Boolean) {
        val userId = db.getCurrentUserId()
        if (userId == 0) return

        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/update_privacy_settings.php"

        val request = object : StringRequest(
            Request.Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        // Settings synced successfully
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Failed to sync, but local settings are saved
                Toast.makeText(this, "Settings saved locally", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "is_private" to if (isPrivate) "1" else "0"
                )
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun performLogout() {
        db.logout()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStop() {
        super.onStop()

        // Check if logout on close is enabled
        if (db.logoutOnClose()) {
            // User is closing the app, logout
            db.logout()
        }
    }
}