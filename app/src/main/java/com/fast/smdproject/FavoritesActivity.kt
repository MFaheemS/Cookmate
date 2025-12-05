package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FavoritesActivity : AppCompatActivity() {

    var recyclerView: androidx.recyclerview.widget.RecyclerView? = null
    var downloadsAdapter: DownloadsAdapter? = null
    var downloadsList = ArrayList<DownloadsModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_downloads)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.downloads)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnHome = findViewById<ImageButton>(R.id.home)
        val searchBtn = findViewById<ImageButton>(R.id.search)
        val btnUpload = findViewById<ImageButton>(R.id.upload)
        val profileBtn = findViewById<ImageButton>(R.id.profile)
        val downloadBtn = findViewById<Button>(R.id.btnDownloads)



        downloadBtn.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        btnHome.setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
        btnUpload.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        searchBtn.setOnClickListener { startActivity(Intent(this, search::class.java)) }
        profileBtn.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    UserProfileActivity::class.java
                )
            )
        }

    }
}