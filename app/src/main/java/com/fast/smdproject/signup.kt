package com.fast.smdproject


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class signup : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignup: Button
    private lateinit var imgProfile: ImageView

    private var encodedImage: String? = null

    private lateinit var login: TextView

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }

                val resizedBitmap = getResizedBitmap(bitmap)  // Resize to prevent OOM
                encodedImage = encodeImage(resizedBitmap)
                imgProfile.setImageBitmap(resizedBitmap)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)


        etUsername = findViewById(R.id.userNameInput)
        etFirstName = findViewById(R.id.firstNameInput)
        etLastName = findViewById(R.id.lastNameInput)
        etEmail = findViewById(R.id.emailInput)
        etPassword = findViewById(R.id.passwordInput)
        btnSignup = findViewById(R.id.createButton)
        imgProfile = findViewById(R.id.profileImage)

        login = findViewById<TextView>(R.id.loginText)

        login.setOnClickListener {

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        imgProfile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSignup.setOnClickListener {
            signupUser()
        }
    }




    private fun encodeImage(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }


    private fun signupUser() {

        val username = etUsername.text.toString().trim()
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()


        if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty() ||
            email.isEmpty() || password.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val ipAddress = getString(R.string.ipAddress)

        val url = "http://$ipAddress/cookMate/signup.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->

                val json = JSONObject(response)

                if (json.getInt("status") == 1) {


                    val db = UserDatabase(this)
                    db.saveUser(
                        username,
                        firstName,
                        lastName,
                        email,
                        encodedImage
                    )

                    Toast.makeText(this, "Signup successful!", Toast.LENGTH_LONG).show()

                    val intent = Intent(this, HomePage::class.java)
                    startActivity(intent)
                    finish()
                }


                Toast.makeText(this, json.getString("message"), Toast.LENGTH_LONG).show()
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["username"] = username
                params["first_name"] = firstName
                params["last_name"] = lastName
                params["email"] = email
                params["password"] = password

                if (encodedImage != null) {
                    params["profile_image"] = encodedImage!!
                }

                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun getResizedBitmap(bitmap: Bitmap, maxSize: Int = 512): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

}
