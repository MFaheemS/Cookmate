package com.fast.smdproject

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var btnAddImage: Button
    private lateinit var recyclerImages: RecyclerView
    private lateinit var description: EditText

    // Multiple Images
    private val selectedImages = mutableListOf<Bitmap>()
    private lateinit var imagePreviewAdapter: ImagePreviewAdapter

    // Image Picker
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                selectedImages.add(bitmap)
                imagePreviewAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
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
        btnAddImage = findViewById(R.id.btn_add_image)
        recyclerImages = findViewById(R.id.recycler_images)
        description = findViewById(R.id.description)

        // Setup RecyclerView for images
        setupImageRecyclerView()

        // Listeners
        btnBack.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                finish()
            }
        }

        btnAddImage.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                if (selectedImages.size < 5) {
                    pickImageLauncher.launch("image/*")
                } else {
                    Toast.makeText(this, "Maximum 5 images allowed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnAddIngredient.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                addIngredientRow()
            }
        }

        btnUpload.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                uploadRecipeToBackend()
            }
        }
    }

    private fun setupImageRecyclerView() {
        recyclerImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        imagePreviewAdapter = ImagePreviewAdapter(selectedImages) { position ->
            selectedImages.removeAt(position)
            imagePreviewAdapter.notifyDataSetChanged()
        }
        recyclerImages.adapter = imagePreviewAdapter
    }

    private fun addIngredientRow() {
        val view = layoutInflater.inflate(R.layout.item_ingredient_row, null)
        ingredientsContainer.addView(view)

        // Add pop-in animation
        com.fast.smdproject.AnimationUtils.popIn(view, 0)
    }

    private fun uploadRecipeToBackend() {
        val title = etTitle.text.toString().trim()
        val tags = etTags.text.toString().trim()
        val desc = description.text.toString().trim()

        // 1. Get Username (User Safety Check)
        val db = UserDatabase(this)
        val currentUsername = db.getUsername() ?: return
        val uniqueId = UUID.randomUUID().toString()

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

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Please add at least one image", Toast.LENGTH_SHORT).show()
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

                    // Convert multiple images to JSON array of base64 strings
                    if (selectedImages.isNotEmpty()) {
                        val imagesArray = JSONArray()
                        for (bitmap in selectedImages) {
                            imagesArray.put(bitmapToBase64(bitmap))
                        }
                        params["images"] = imagesArray.toString()
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
            // For offline save, convert first image to base64
            val imageBase64 = if (selectedImages.isNotEmpty()) {
                bitmapToBase64(selectedImages[0])
            } else {
                ""
            }

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