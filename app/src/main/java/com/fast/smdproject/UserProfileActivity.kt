package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class UserProfileActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val editBtn = findViewById<ImageView>(R.id.ivEdit)

        val btnHome = findViewById<ImageButton>(R.id.home)
        val searchBtn = findViewById<ImageButton>(R.id.search)
        val btnUpload = findViewById<ImageButton>(R.id.upload)
        val libBtn = findViewById<ImageButton>(R.id.lib)
        val settings = findViewById<ImageView>(R.id.ivSettings)

        settings.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }




        libBtn.setOnClickListener { startActivity(Intent(this, DownloadsActivity::class.java)) }
        btnHome.setOnClickListener { startActivity(Intent(this, HomePage::class.java)) }
        btnUpload.setOnClickListener { startActivity(Intent(this, UploadRecipe::class.java)) }
        searchBtn.setOnClickListener { startActivity(Intent(this, search::class.java)) }


        editBtn.setOnClickListener {
            startActivity(
                android.content.Intent(
                    this,
                    EditUserProfileActivity::class.java
                )
            )
        }

    }
}