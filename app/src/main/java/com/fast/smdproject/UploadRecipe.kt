package com.fast.smdproject

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID

class UploadRecipe : AppCompatActivity() {


    private lateinit var etTitle: EditText
    private lateinit var etTags: EditText
    private lateinit var etFirstStep: EditText
    private lateinit var btnAddIngredient: Button
    private lateinit var btnUpload: Button
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var btnBack: ImageView

    // Single Image UI
    private lateinit var imgRecipePreview: ImageView
    private lateinit var txtTapHint: TextView
    private lateinit var btnRemoveImage: ImageView

    // Data Variables
    private var selectedImageBitmap: Bitmap? = null

    private lateinit var description: EditText

    // Image Picker
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                // Convert Uri to Bitmap immediately
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                selectedImageBitmap = bitmap
                updateImageUI()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_recipe)

        // Init Views
        etTitle = findViewById(R.id.edit_title)
        etTags = findViewById(R.id.edit_tags)
        etFirstStep = findViewById(R.id.edit_step)
        btnAddIngredient = findViewById(R.id.btn_add_ingredient)
        btnUpload = findViewById(R.id.btn_upload)
        ingredientsContainer = findViewById(R.id.ingredients_container)
        btnBack = findViewById(R.id.btn_back)

        imgRecipePreview = findViewById(R.id.img_recipe_preview)
        txtTapHint = findViewById(R.id.txt_tap_hint)
        btnRemoveImage = findViewById(R.id.btn_remove_image)

        description = findViewById(R.id.description)

        // Listeners
        btnBack.setOnClickListener { finish() }

        imgRecipePreview.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnRemoveImage.setOnClickListener {
            selectedImageBitmap = null
            updateImageUI()
        }

        btnAddIngredient.setOnClickListener {
            addIngredientRow()
        }

        btnUpload.setOnClickListener {
            uploadRecipeToBackend()
        }
    }

    private fun updateImageUI() {
        if (selectedImageBitmap != null) {
            imgRecipePreview.setImageBitmap(selectedImageBitmap)
            txtTapHint.visibility = View.GONE
            btnRemoveImage.visibility = View.VISIBLE
        } else {
            imgRecipePreview.setImageResource(R.drawable.biryani) // Your placeholder
            txtTapHint.visibility = View.VISIBLE
            btnRemoveImage.visibility = View.GONE
        }
    }

    private fun addIngredientRow() {
        val view = layoutInflater.inflate(R.layout.item_ingredient_row, null)
        ingredientsContainer.addView(view)
    }

    private fun uploadRecipeToBackend() {
        val title = etTitle.text.toString().trim()
        val tags = etTags.text.toString().trim()
        val desc = description.text.toString().trim()

        // 1. Get Username (User Safety Check)
        val db = UserDatabase(this)
        val currentUsername = db.getUsername() ?: return
        val uniqueId = UUID.randomUUID().toString()
        val imageBase64 = if (selectedImageBitmap != null) bitmapToBase64(selectedImageBitmap!!) else ""

        if (currentUsername.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }



        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/uploadRecipe.php"

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        // --- FIXED: SAFE INGREDIENT LOOP ---
        val ingredientsArray = JSONArray()
        for (i in 0 until ingredientsContainer.childCount) {
            val row = ingredientsContainer.getChildAt(i)

            // use '?' to avoid crash if IDs are missing or view is wrong
            val etName = row.findViewById<EditText>(R.id.row_ing_name)
            val etQty = row.findViewById<EditText>(R.id.row_ing_qty)

            // Only add if both fields exist and name is not empty
            if (etName != null && etQty != null) {
                val nameText = etName.text.toString().trim()
                val qtyText = etQty.text.toString().trim()

                if (nameText.isNotEmpty()) {
                    val obj = JSONObject()
                    obj.put("name", nameText)
                    obj.put("qty", qtyText)
                    ingredientsArray.put(obj)
                }
            }
        }

        // --- FIXED: SAFE STEPS ARRAY ---
        val stepsArray = JSONArray()
        // Check if etFirstStep is initialized and has text
        if (etFirstStep.text != null && etFirstStep.text.toString().isNotEmpty()) {
            stepsArray.put(etFirstStep.text.toString().trim())
        }

        // Convert to Strings BEFORE the request to avoid threading issues
        val finalIngredientsJson = ingredientsArray.toString()
        val finalStepsJson = stepsArray.toString()

        if(isNetworkAvailable()){

            val request = object : StringRequest(
                Request.Method.POST, url,
                { response ->
                    try {
                        val json = JSONObject(response)
                        if (json.getString("status") == "success") {
                            Toast.makeText(this, "Upload Successful!", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            Toast.makeText(this, json.getString("message"), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "JSON Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    val params = HashMap<String, String>()
                    params["username"] = currentUsername
                    params["unique_id"] = UUID.randomUUID().toString()
                    params["title"] = title
                    params["tags"] = tags

                    // Use the safe strings we created above
                    params["ingredients"] = finalIngredientsJson
                    params["steps"] = finalStepsJson
                    params["description"] = desc

                    if (selectedImageBitmap != null) {
                        params["image"] = bitmapToBase64(selectedImageBitmap!!)
                    }

                    return params
                }
            }

            request.retryPolicy = DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )

            Volley.newRequestQueue(this).add(request)
        }

        else{

            db.savePendingRecipe(
                uniqueId, currentUsername, title, desc,
                finalIngredientsJson, finalStepsJson, tags, imageBase64
            )

            Toast.makeText(this, "No Internet. Saved to Uploads! Will upload automatically.", Toast.LENGTH_LONG).show()


            scheduleUploadWorker()

            finish()
        }
    }

    // Helper: Bitmap -> Base64 String
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun scheduleUploadWorker() {
        val uploadWorkRequest = androidx.work.OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()

        androidx.work.WorkManager.getInstance(this).enqueue(uploadWorkRequest)
    }
}