package com.fast.smdproject

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.HorizontalScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.flexbox.FlexboxLayout
import org.json.JSONArray
import org.json.JSONException

class SearchRecipeActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var btnBack: ImageView
    private lateinit var btnFilter: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var filterChipsContainer: HorizontalScrollView
    private lateinit var filterChipsLayout: FlexboxLayout
    private lateinit var recipeAdapter: SearchRecipeAdapter
    private val searchList = ArrayList<Recipe>()
    private var currentCategories = mutableListOf<String>()
    private var currentIngredients = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val btnHome = findViewById<ImageView>(R.id.home)
        val btnSearch = findViewById<ImageView>(R.id.search)
        val btnUpload = findViewById<ImageView>(R.id.upload)
        val libBtn = findViewById<ImageView>(R.id.lib)
        val profileBtn = findViewById<ImageView>(R.id.profile)

        btnHome.setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
        btnSearch.setOnClickListener { startActivity(Intent(this, SearchUserActivity::class.java)) }
        btnUpload.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        libBtn.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        profileBtn.setOnClickListener { startActivity(Intent(this, UserProfileActivity::class.java)) }

        searchInput = findViewById(R.id.search_input)
        btnBack = findViewById(R.id.btn_back)
        btnFilter = findViewById(R.id.btn_filter)
        recyclerView = findViewById(R.id.recycler_search_results)
        filterChipsContainer = findViewById(R.id.filter_chips_container)
        filterChipsLayout = findViewById(R.id.filter_chips_layout)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val ipAddress = getString(R.string.ipAddress)

        recipeAdapter = SearchRecipeAdapter(this, searchList, ipAddress, currentCategories)
        recyclerView.adapter = recipeAdapter

        btnBack.setOnClickListener {
            finish()
        }

        btnFilter.setOnClickListener {
            showCategoryFilterDialog()
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearchWithFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showCategoryFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category_filter, null)
        val filterInputCategories = dialogView.findViewById<EditText>(R.id.filter_input_categories)
        val filterInputIngredients = dialogView.findViewById<EditText>(R.id.filter_input_ingredients)
        val btnCategoriesTab = dialogView.findViewById<Button>(R.id.btn_categories_tab)
        val btnIngredientsTab = dialogView.findViewById<Button>(R.id.btn_ingredients_tab)
        val categoriesSection = dialogView.findViewById<android.view.View>(R.id.categories_section)
        val ingredientsSection = dialogView.findViewById<android.view.View>(R.id.ingredients_section)
        val btnClear = dialogView.findViewById<Button>(R.id.btn_clear)
        val btnApply = dialogView.findViewById<Button>(R.id.btn_apply)

        var isShowingCategories = true

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Tab switching logic
        btnCategoriesTab.setOnClickListener {
            AnimationUtils.buttonPressEffect(it) {
                if (!isShowingCategories) {
                    isShowingCategories = true
                    categoriesSection.visibility = android.view.View.VISIBLE
                    ingredientsSection.visibility = android.view.View.GONE
                    btnCategoriesTab.setBackgroundResource(R.drawable.button_green_rounded)
                    btnCategoriesTab.setTextColor(android.graphics.Color.WHITE)
                    btnIngredientsTab.setBackgroundResource(R.drawable.button_rounded_gray)
                    btnIngredientsTab.setTextColor(android.graphics.Color.parseColor("#666666"))
                }
            }
        }

        btnIngredientsTab.setOnClickListener {
            AnimationUtils.buttonPressEffect(it) {
                if (isShowingCategories) {
                    isShowingCategories = false
                    categoriesSection.visibility = android.view.View.GONE
                    ingredientsSection.visibility = android.view.View.VISIBLE
                    btnIngredientsTab.setBackgroundResource(R.drawable.button_green_rounded)
                    btnIngredientsTab.setTextColor(android.graphics.Color.WHITE)
                    btnCategoriesTab.setBackgroundResource(R.drawable.button_rounded_gray)
                    btnCategoriesTab.setTextColor(android.graphics.Color.parseColor("#666666"))
                }
            }
        }

        btnClear.setOnClickListener {
            AnimationUtils.buttonPressEffect(it) {
                currentCategories.clear()
                currentIngredients.clear()
                searchInput.setText("")
                searchList.clear()
                updateChipsUI()
                recipeAdapter = SearchRecipeAdapter(this, searchList, getString(R.string.ipAddress), currentCategories)
                recyclerView.adapter = recipeAdapter
                dialog.dismiss()
                Toast.makeText(this, "Filter cleared", Toast.LENGTH_SHORT).show()
            }
        }

        btnApply.setOnClickListener {
            AnimationUtils.buttonPressEffect(it) {
                if (isShowingCategories) {
                    // Apply category filter
                    val input = filterInputCategories.text.toString().trim()
                    if (input.isNotEmpty()) {
                        val newCategories = input.split(",")
                            .map { it.trim().removePrefix("#").lowercase() }
                            .filter { it.isNotEmpty() }

                        if (newCategories.isNotEmpty()) {
                            newCategories.forEach { category ->
                                if (!currentCategories.contains(category)) {
                                    currentCategories.add(category)
                                }
                            }
                            performSearchWithFilters()
                            updateChipsUI()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(this, "Please enter valid categories", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Please enter categories", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Apply ingredient filter
                    val input = filterInputIngredients.text.toString().trim()
                    if (input.isNotEmpty()) {
                        val newIngredients = input.split(",")
                            .map { it.trim().lowercase() }
                            .filter { it.isNotEmpty() }

                        if (newIngredients.isNotEmpty()) {
                            newIngredients.forEach { ingredient ->
                                if (!currentIngredients.contains(ingredient)) {
                                    currentIngredients.add(ingredient)
                                }
                            }
                            performSearchWithFilters()
                            updateChipsUI()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(this, "Please enter valid ingredients", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Please enter ingredients", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun performSearchWithFilters() {
        val ipAddress = getString(R.string.ipAddress)
        val query = searchInput.text.toString().trim()

        // Build URL with query, categories, and ingredients
        var url = "http://$ipAddress/cookMate/search_recipes.php?"

        if (query.isNotEmpty()) {
            url += "query=${java.net.URLEncoder.encode(query, "UTF-8")}"
        }

        if (currentCategories.isNotEmpty()) {
            if (query.isNotEmpty()) {
                url += "&"
            }
            val categoriesParam = currentCategories.joinToString(",")
            url += "categories=${java.net.URLEncoder.encode(categoriesParam, "UTF-8")}"
        }

        if (currentIngredients.isNotEmpty()) {
            if (query.isNotEmpty() || currentCategories.isNotEmpty()) {
                url += "&"
            }
            val ingredientsParam = currentIngredients.joinToString(",")
            url += "ingredients=${java.net.URLEncoder.encode(ingredientsParam, "UTF-8")}"
        }

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val jsonArray = response.getJSONArray("data")
                        searchList.clear()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)

                            // Parse images - extract first image as cover
                            val imagesString = obj.getString("images")
                            val coverImage = try {
                                val imagesArray = JSONArray(imagesString)
                                if (imagesArray.length() > 0) {
                                    imagesArray.getString(0)
                                } else {
                                    ""
                                }
                            } catch (e: Exception) {
                                imagesString
                            }

                            val recipe = Recipe(
                                recipeId = obj.getInt("recipe_id"),
                                title = obj.getString("title"),
                                description = obj.getString("description"),
                                tags = obj.getString("tags"),
                                imagePath = coverImage
                            )
                            searchList.add(recipe)
                        }

                        recipeAdapter = SearchRecipeAdapter(this, searchList, ipAddress, currentCategories)
                        recyclerView.adapter = recipeAdapter
                    } else {
                        searchList.clear()
                        recipeAdapter.notifyDataSetChanged()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error parsing results", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun performCategorySearch(categories: List<String>) {
        performSearchWithFilters()
    }

    private fun performSearch(query: String) {
        performSearchWithFilters()
    }

    private fun updateChipsUI() {
        filterChipsLayout.removeAllViews()

        if (currentCategories.isEmpty() && currentIngredients.isEmpty()) {
            filterChipsContainer.visibility = View.GONE
            return
        }

        filterChipsContainer.visibility = View.VISIBLE

        var chipIndex = 0

        // Add category chips
        currentCategories.forEach { category ->
            val chipView = LayoutInflater.from(this).inflate(R.layout.item_category_chip, filterChipsLayout, false)
            val chipText = chipView.findViewById<TextView>(R.id.chip_text)
            val chipClose = chipView.findViewById<ImageView>(R.id.chip_close)

            chipText.text = "#$category"
            chipText.setTextColor(android.graphics.Color.WHITE) // White text for categories
            // Use drawable resource to maintain rounded corners - Green for categories
            chipView.setBackgroundResource(R.drawable.chip_background_green)

            // Add pop-in animation with stagger
            AnimationUtils.popIn(chipView, chipIndex * 50L)
            chipIndex++

            chipClose.setOnClickListener {
                AnimationUtils.buttonPressEffect(it) {
                    removeCategoryWithAnimation(category, chipView)
                }
            }

            filterChipsLayout.addView(chipView)
        }

        // Add ingredient chips
        currentIngredients.forEach { ingredient ->
            val chipView = LayoutInflater.from(this).inflate(R.layout.item_category_chip, filterChipsLayout, false)
            val chipText = chipView.findViewById<TextView>(R.id.chip_text)
            val chipClose = chipView.findViewById<ImageView>(R.id.chip_close)

            chipText.text = ingredient
            chipText.setTextColor(android.graphics.Color.WHITE) // White text for ingredients
            // Use drawable resource to maintain rounded corners - Light green for ingredients
            chipView.setBackgroundResource(R.drawable.chip_background_light_green)

            // Add pop-in animation with stagger
            AnimationUtils.popIn(chipView, chipIndex * 50L)
            chipIndex++

            chipClose.setOnClickListener {
                AnimationUtils.buttonPressEffect(it) {
                    removeIngredientWithAnimation(ingredient, chipView)
                }
            }

            filterChipsLayout.addView(chipView)
        }
    }

    private fun removeCategoryWithAnimation(category: String, chipView: View) {
        AnimationUtils.popOut(chipView) {
            currentCategories.remove(category)
            performSearchWithFilters()

            if (currentCategories.isEmpty() && currentIngredients.isEmpty()) {
                filterChipsContainer.visibility = View.GONE
            }
        }
    }

    private fun removeIngredientWithAnimation(ingredient: String, chipView: View) {
        AnimationUtils.popOut(chipView) {
            currentIngredients.remove(ingredient)
            performSearchWithFilters()

            if (currentCategories.isEmpty() && currentIngredients.isEmpty()) {
                filterChipsContainer.visibility = View.GONE
            }
        }
    }

    private fun removeCategory(category: String) {
        currentCategories.remove(category)
        updateChipsUI()

        // Perform search with remaining categories and query
        performSearchWithFilters()
    }
}
