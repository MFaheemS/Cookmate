package com.fast.smdproject

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homepage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnSearch = findViewById<ImageView>(R.id.search)
        val btnUpload = findViewById<ImageView>(R.id.upload)
        val btnLib = findViewById<ImageView>(R.id.lib)
        val btnProfile = findViewById<ImageView>(R.id.profile)

        btnSearch.setOnClickListener {

            val intent = android.content.Intent(this, search::class.java)
            startActivity(intent)
        }

        btnUpload.setOnClickListener {

            val intent = android.content.Intent(this, UploadRecipe::class.java)
            startActivity(intent)
        }

    }
}