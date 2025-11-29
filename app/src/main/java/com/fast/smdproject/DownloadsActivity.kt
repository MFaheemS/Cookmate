package com.fast.smdproject

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DownloadsActivity : AppCompatActivity() {

    var recyclerView : androidx.recyclerview.widget.RecyclerView? = null
    var downloadsAdapter : DownloadsAdapter? = null
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


        downloadsList.add(DownloadsModel("Spaghetti Carbonara", "Pasta", "A classic Italian pasta dish made with eggs, cheese, pancetta, and pepper.", null, R.drawable.ic_like_fill, R.drawable.ic_download, R.drawable.ic_timer))
        downloadsList.add(DownloadsModel("Margherita Pizza", "Pizza", "A simple yet delicious pizza topped with fresh tomatoes, mozzarella cheese, and basil.", null, R.drawable.ic_like, R.drawable.ic_downloaded, R.drawable.ic_timer))
        downloadsList.add(DownloadsModel("Caesar Salad", "Salad", "A refreshing salad made with romaine lettuce, croutons, Parmesan cheese, and Caesar dressing.", null, R.drawable.ic_like_fill, R.drawable.ic_download, R.drawable.ic_timer))
        downloadsList.add(DownloadsModel("Beef Tacos", "Mexican", "Soft tortillas filled with seasoned beef, lettuce, cheese, and salsa.", null, R.drawable.ic_like, R.drawable.ic_downloaded, R.drawable.ic_timer))
        downloadsList.add(DownloadsModel("Chicken Curry", "Indian", "A flavorful curry dish made with tender chicken pieces simmered in a spiced tomato and cream sauce.", null, R.drawable.ic_like_fill, R.drawable.ic_download, R.drawable.ic_timer))


        recyclerView = findViewById<RecyclerView>(R.id.downloadsRecyclerView)
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        downloadsAdapter = DownloadsAdapter(this, downloadsList)
        recyclerView!!.adapter = downloadsAdapter
    }
}