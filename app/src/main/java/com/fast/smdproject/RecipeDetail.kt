package com.fast.smdproject

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONArray
import org.json.JSONObject

class RecipeDetail : AppCompatActivity() {

    // UI Components
    private lateinit var imgRecipe: ImageView
    private lateinit var txtTitle: TextView
    private lateinit var txtDesc: TextView
    private lateinit var imgUser: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var tagsContainer: LinearLayout
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout
    private lateinit var btnBack: ImageView

    private lateinit var btnFollow : Button

    private var db: UserDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        // Init Views
        imgRecipe = findViewById(R.id.recipe_image)
        txtTitle = findViewById(R.id.recipe_title)
        txtDesc = findViewById(R.id.recipe_desc)
        imgUser = findViewById(R.id.user_avatar)
        txtUsername = findViewById(R.id.username)
        tagsContainer = findViewById(R.id.tags_container)
        ingredientsContainer = findViewById(R.id.ingredients_list_container)
        stepsContainer = findViewById(R.id.steps_list_container)
        btnBack = findViewById(R.id.btn_back)
        btnFollow = findViewById(R.id.btn_follow)

        btnBack.setOnClickListener { finish() }

        db = UserDatabase(this)


        val recipeId = intent.getIntExtra("RECIPE_ID", -1)

        if (recipeId != -1) {
            fetchRecipeDetails(recipeId)
        } else {
            Toast.makeText(this, "Error: Invalid Recipe ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchRecipeDetails(id: Int) {
        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/get_recipe_detail.php"

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val data = json.getJSONObject("data")
                        populateUI(data, ipAddress)
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
                params["recipe_id"] = id.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun populateUI(data: JSONObject, ipAddress: String) {
        // 1. Basic Info
        txtTitle.text = data.getString("title")
        txtDesc.text = data.getString("description")

        if(db != null &&  db!!.getUsername() == data.getString("username")){
            txtUsername.text = "You"
            btnFollow.visibility = View.GONE
        }

        else{
            txtUsername.text = data.getString("username")
            btnFollow.visibility = View.VISIBLE
        }

        // 2. Load Hero Image
        val imagePath = data.getString("images")
        if (!imagePath.isNullOrEmpty()) {
            val fullUrl = "http://$ipAddress/cookMate/$imagePath"
            Glide.with(this).load(fullUrl).into(imgRecipe)
        }


        val userImg = data.getString("profile_image")
        if (!userImg.isNullOrEmpty()) {

            val fullUrl = "http://$ipAddress/cookMate/$userImg"

            Glide.with(this).load(fullUrl).into(imgUser)
        }

        else{

            imgUser.setImageResource(R.drawable.profile)
        }

        // 4. Populate Tags
        val tagsString = data.getString("tags")
        val tagsList = tagsString.split(",")
        tagsContainer.removeAllViews()
        for (tag in tagsList) {
            val textView = TextView(this)
            textView.text = tag.trim()
            textView.setTextColor(resources.getColor(R.color.white))
            textView.textSize = 12f
            textView.setPadding(20, 10, 20, 10)
            tagsContainer.addView(textView)
        }

        // 5. Populate Ingredients (JSON Array)
        val ingredientsJson = data.getString("ingredients")
        val ingArray = JSONArray(ingredientsJson)
        ingredientsContainer.removeAllViews()

        for (i in 0 until ingArray.length()) {
            val obj = ingArray.getJSONObject(i)
            // Inflate our helper row
            val view = layoutInflater.inflate(R.layout.item_ingredient_display, null)
            val txtName = view.findViewById<TextView>(R.id.txt_ing_name)
            val txtQty = view.findViewById<TextView>(R.id.txt_ing_qty)

            txtName.text = obj.getString("name")
            txtQty.text = obj.getString("qty")

            ingredientsContainer.addView(view)
        }

        // 6. Populate Steps (JSON Array)
        val stepsJson = data.getString("steps")
        val stepsArray = JSONArray(stepsJson)
        stepsContainer.removeAllViews()

        for (i in 0 until stepsArray.length()) {
            val stepDesc = stepsArray.getString(i)

            val view = layoutInflater.inflate(R.layout.item_step_display, null)
            val txtNum = view.findViewById<TextView>(R.id.txt_step_number)
            val txtDesc = view.findViewById<TextView>(R.id.txt_step_desc)

            txtNum.text = "Step ${i + 1}"
            txtDesc.text = stepDesc

            stepsContainer.addView(view)
        }
    }
}