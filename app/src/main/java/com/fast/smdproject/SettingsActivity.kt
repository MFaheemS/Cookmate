package com.fast.smdproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SettingsActivity  : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val logOutBtn = findViewById<MaterialButton>(R.id.btnLogout)

        logOutBtn.setOnClickListener {

            val db = UserDatabase(this)
            db.logout()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}