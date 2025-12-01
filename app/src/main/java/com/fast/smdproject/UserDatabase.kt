package com.fast.smdproject

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDatabase(context: Context) :
    SQLiteOpenHelper(context, "user_db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE user (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT," +
                    "first_name TEXT," +
                    "last_name TEXT," +
                    "email TEXT," +
                    "profile_image TEXT" +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        db.execSQL("DROP TABLE IF EXISTS user")
        onCreate(db)
    }

    fun saveUser(username: String, first: String, last: String, email: String, image: String?) {
        val db = writableDatabase
        db.execSQL("DELETE FROM user")  // allow only 1 logged user

        val profileImage = image ?: ""

        db.execSQL(
            "INSERT INTO user (username, first_name, last_name, email, profile_image) VALUES (?, ?, ?, ?, ?)",
            arrayOf(username, first, last, email, profileImage)
        )
        db.close()
    }

    fun isLoggedIn(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM user LIMIT 1", null)
        val loggedIn = cursor.count > 0
        cursor.close()
        db.close()
        return loggedIn
    }
}
