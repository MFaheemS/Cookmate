package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

import android.util.Base64
import com.android.volley.toolbox.ImageRequest
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var signUpText: TextView
    private lateinit var forgotPasswordText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etUsername = findViewById(R.id.usernameInput)
        etPassword = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.loginButton)
        signUpText = findViewById(R.id.signUpText)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)

        signUpText.setOnClickListener {
            startActivity(Intent(this, signup::class.java))
        }

        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, forgetscreen::class.java))
        }

        btnLogin.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password.", Toast.LENGTH_SHORT).show()
            return
        }

        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/login.php"

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val status = json.optInt("status", 0)
                    val message = json.optString("message", "Unknown response")

                    if (status == 1) {
                        val userObj = json.optJSONObject("user")
                        if (userObj != null) {
                            val userId = userObj.optInt("user_id", 0)
                            val usernameResp = userObj.optString("username", "")
                            val firstName = userObj.optString("first_name", "")
                            val lastName = userObj.optString("last_name", "")
                            val email = userObj.optString("email", "")
                            val profileImagePath = userObj.optString("profile_image", "")

                            if (!profileImagePath.isNullOrEmpty()) {
                                val imageUrl = "http://$ipAddress/cookMate/$profileImagePath"

                                val imageRequest = ImageRequest(
                                    imageUrl,
                                    { bitmap ->

                                        val baos = ByteArrayOutputStream()
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                                        val imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)


                                        val db = UserDatabase(this)
                                        db.saveUser(userId, usernameResp, firstName, lastName, email, imageBase64)

                                        // Initialize FCM after login
                                        FCMHelper.sendPendingToken(this)

                                        Toast.makeText(this, "Login successful.", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, HomePage::class.java))
                                        finish()
                                    },
                                    0, 0, null,
                                    { error ->

                                        val db = UserDatabase(this)
                                        db.saveUser(userId, usernameResp, firstName, lastName, email, null)

                                        // Initialize FCM after login
                                        FCMHelper.sendPendingToken(this)

                                        Toast.makeText(this, "Login successful (image not loaded).", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, HomePage::class.java))
                                        finish()
                                    }
                                )
                                Volley.newRequestQueue(this).add(imageRequest)
                            } else {

                                val db = UserDatabase(this)
                                db.saveUser(userId, usernameResp, firstName, lastName, email, null)

                                // Initialize FCM after login
                                FCMHelper.sendPendingToken(this)

                                Toast.makeText(this, "Login successful.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, HomePage::class.java))
                                finish()
                            }
                        } else {
                            Toast.makeText(this, "Malformed user data.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Parse error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["username"] = username
                params["password"] = password
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}
