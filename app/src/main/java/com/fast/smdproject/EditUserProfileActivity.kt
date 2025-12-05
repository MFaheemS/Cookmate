package com.fast.smdproject

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class EditUserProfileActivity : AppCompatActivity() {

    // UI Components
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var ivProfile: ImageView
    private lateinit var btnChangePhoto: RelativeLayout
    private lateinit var btnSave: TextView
    private lateinit var btnBack: ImageView

    // Data
    private var selectedBitmap: Bitmap? = null
    private var currentUsername: String = "" // Need this to identify user in DB

    // Image Picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                selectedBitmap = bitmap
                ivProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user_profile)

        initViews()
        loadCurrentUserData()

        // Listeners
        btnBack.setOnClickListener { finish() }

        btnChangePhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        btnSave.setOnClickListener {
            updateProfile()
        }
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        ivProfile = findViewById(R.id.ivProfile)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        btnSave = findViewById(R.id.ivSave)
        btnBack = findViewById(R.id.ivSettings) // Reused ID from your XML
    }

    private fun loadCurrentUserData() {
        val db = UserDatabase(this)
        val userDetails = db.getUserDetails()

        if (userDetails.isNotEmpty()) {
            currentUsername = userDetails["username"] ?: ""


            etUsername.setText(currentUsername)
            etEmail.setText(userDetails["email"])


            val encodedImage = userDetails["profile_image"]
            if (!encodedImage.isNullOrEmpty()) {
                try {
                    val decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT)
                    val decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    ivProfile.setImageBitmap(decodedBitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateProfile() {
        val newUsername = etUsername.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()
        val newPassword = etPassword.text.toString().trim()

        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/update_profile.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getInt("status") == 1) {
                        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()


                        val db = UserDatabase(this@EditUserProfileActivity)


                        var imageString: String? = null
                        if (selectedBitmap != null) {
                            imageString = bitmapToBase64(selectedBitmap!!)
                        }


                        db.updateLocalProfile(newUsername, newEmail, imageString)

                        finish()
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
                params["original_username"] = currentUsername

                if (newUsername.isNotEmpty()) params["username"] = newUsername
                if (newEmail.isNotEmpty())    params["email"] = newEmail
                if (newPassword.isNotEmpty()) params["password"] = newPassword

                if (selectedBitmap != null) {
                    params["profile_image"] = bitmapToBase64(selectedBitmap!!)
                }

                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
}