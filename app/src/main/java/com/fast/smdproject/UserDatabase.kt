package com.fast.smdproject

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDatabase(context: Context) :
    SQLiteOpenHelper(context, "user_db", null, 2) { // Changed version to 2

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


        createPendingTable(db)
    }

    private fun createPendingTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE pending_recipes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "unique_id TEXT," +
                    "username TEXT," +
                    "title TEXT," +
                    "description TEXT," +
                    "ingredients TEXT," +
                    "steps TEXT," +
                    "tags TEXT," +
                    "image_base64 TEXT" +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        if (oldV < 2) {
            createPendingTable(db)
        }
    }


    fun saveUser(username: String, first: String, last: String, email: String, image: String?) {
        val db = writableDatabase
        db.execSQL("DELETE FROM user")
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

    fun getUsername(): String? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT username FROM user LIMIT 1", null)
        var username: String? = null
        if (cursor.moveToFirst()) {
            username = cursor.getString(0)
        }
        cursor.close()
        db.close()
        return username
    }



    fun savePendingRecipe(uniqueId: String, username: String, title: String, desc: String,
                          ingredients: String, steps: String, tags: String, imageBase64: String) {
        val db = writableDatabase
        val values = ContentValues()
        values.put("unique_id", uniqueId)
        values.put("username", username)
        values.put("title", title)
        values.put("description", desc)
        values.put("ingredients", ingredients)
        values.put("steps", steps)
        values.put("tags", tags)
        values.put("image_base64", imageBase64)

        db.insert("pending_recipes", null, values)
        db.close()
    }

    fun getPendingRecipes(): ArrayList<HashMap<String, String>> {
        val list = ArrayList<HashMap<String, String>>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM pending_recipes", null)

        if (cursor.moveToFirst()) {
            do {
                val map = HashMap<String, String>()
                map["id"] = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                map["unique_id"] = cursor.getString(cursor.getColumnIndexOrThrow("unique_id"))
                map["username"] = cursor.getString(cursor.getColumnIndexOrThrow("username"))
                map["title"] = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                map["description"] = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                map["ingredients"] = cursor.getString(cursor.getColumnIndexOrThrow("ingredients"))
                map["steps"] = cursor.getString(cursor.getColumnIndexOrThrow("steps"))
                map["tags"] = cursor.getString(cursor.getColumnIndexOrThrow("tags"))
                map["image_base64"] = cursor.getString(cursor.getColumnIndexOrThrow("image_base64"))
                list.add(map)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun deletePendingRecipe(id: String) {
        val db = writableDatabase
        db.delete("pending_recipes", "id=?", arrayOf(id))
        db.close()
    }


    fun getUserDetails(): HashMap<String, String> {
        val db = readableDatabase

        val cursor = db.rawQuery("SELECT username, email, first_name, last_name, profile_image FROM user LIMIT 1", null)

        val userMap = HashMap<String, String>()

        if (cursor.moveToFirst()) {
            userMap["username"] = cursor.getString(cursor.getColumnIndexOrThrow("username"))
            userMap["email"] = cursor.getString(cursor.getColumnIndexOrThrow("email"))
            userMap["first_name"] = cursor.getString(cursor.getColumnIndexOrThrow("first_name"))
            userMap["last_name"] = cursor.getString(cursor.getColumnIndexOrThrow("last_name"))


            val img = cursor.getString(cursor.getColumnIndexOrThrow("profile_image"))
            userMap["profile_image"] = img ?: ""
        }

        cursor.close()
        db.close()
        return userMap
    }


    fun updateLocalProfile(newUsername: String?, newEmail: String?, newImage: String?) {
        val db = writableDatabase
        val values = ContentValues()


        if (!newUsername.isNullOrEmpty()) values.put("username", newUsername)
        if (!newEmail.isNullOrEmpty()) values.put("email", newEmail)
        if (!newImage.isNullOrEmpty()) values.put("profile_image", newImage)


        db.update("user", values, null, null)
        db.close()
    }


    fun logout() {
        val db = writableDatabase

        db.execSQL("DELETE FROM user")
        db.close()
    }
}