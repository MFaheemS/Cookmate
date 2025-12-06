package com.fast.smdproject

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDatabase(context: Context) :
    SQLiteOpenHelper(context, "user_db", null, 4) { // Changed version to 4

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            "CREATE TABLE user (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "username TEXT," +
                    "first_name TEXT," +
                    "last_name TEXT," +
                    "email TEXT," +
                    "profile_image TEXT" +
                    ")"
        )

        createPendingTable(db)
        createFollowersTable(db)
        createFollowingTable(db)
        createSyncInfoTable(db)
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

    private fun createFollowersTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE cached_followers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "username TEXT," +
                    "first_name TEXT," +
                    "last_name TEXT," +
                    "profile_image TEXT" +
                    ")"
        )
    }

    private fun createFollowingTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE cached_following (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "username TEXT," +
                    "first_name TEXT," +
                    "last_name TEXT," +
                    "profile_image TEXT" +
                    ")"
        )
    }

    private fun createSyncInfoTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE sync_info (" +
                    "id INTEGER PRIMARY KEY," +
                    "type TEXT," +
                    "last_sync_time INTEGER" +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        if (oldV < 2) {
            createPendingTable(db)
        }
        if (oldV < 3) {
            // Add user_id column if upgrading from version 2
            db.execSQL("ALTER TABLE user ADD COLUMN user_id INTEGER")
        }
        if (oldV < 4) {
            createFollowersTable(db)
            createFollowingTable(db)
            createSyncInfoTable(db)
        }
    }


    fun saveUser(userId: Int, username: String, first: String, last: String, email: String, image: String?) {
        val db = writableDatabase
        db.execSQL("DELETE FROM user")
        val profileImage = image ?: ""
        db.execSQL(
            "INSERT INTO user (user_id, username, first_name, last_name, email, profile_image) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(userId, username, first, last, email, profileImage)
        )
        db.close()
    }

    fun getCurrentUserId(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT user_id FROM user LIMIT 1", null)
        var userId = 0
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return userId
    }

    fun getUserInfo(): Map<String, String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT username, first_name, last_name, profile_image FROM user LIMIT 1", null)
        val info = mutableMapOf<String, String>()
        if (cursor.moveToFirst()) {
            info["username"] = cursor.getString(0) ?: ""
            info["first_name"] = cursor.getString(1) ?: ""
            info["last_name"] = cursor.getString(2) ?: ""
            info["profile_image"] = cursor.getString(3) ?: ""
        }
        cursor.close()
        db.close()
        return info
    }

    // Followers methods
    fun saveFollowers(followers: List<User>) {
        val db = writableDatabase
        db.execSQL("DELETE FROM cached_followers")

        followers.forEach { user ->
            db.execSQL(
                "INSERT INTO cached_followers (user_id, username, first_name, last_name, profile_image) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any>(user.userId, user.username, user.firstName, user.lastName, user.profileImage ?: "")
            )
        }

        // Update sync time
        val currentTime = System.currentTimeMillis()
        db.execSQL("DELETE FROM sync_info WHERE type = 'followers'")
        db.execSQL("INSERT INTO sync_info (type, last_sync_time) VALUES ('followers', ?)", arrayOf<Any>(currentTime))

        db.close()
    }

    fun getCachedFollowers(): List<User> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT user_id, username, first_name, last_name, profile_image FROM cached_followers", null)
        val followers = mutableListOf<User>()

        while (cursor.moveToNext()) {
            followers.add(
                User(
                    userId = cursor.getInt(0),
                    username = cursor.getString(1),
                    firstName = cursor.getString(2),
                    lastName = cursor.getString(3),
                    profileImage = cursor.getString(4)
                )
            )
        }

        cursor.close()
        db.close()
        return followers
    }

    // Following methods
    fun saveFollowing(following: List<User>) {
        val db = writableDatabase
        db.execSQL("DELETE FROM cached_following")

        following.forEach { user ->
            db.execSQL(
                "INSERT INTO cached_following (user_id, username, first_name, last_name, profile_image) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any>(user.userId, user.username, user.firstName, user.lastName, user.profileImage ?: "")
            )
        }

        // Update sync time
        val currentTime = System.currentTimeMillis()
        db.execSQL("DELETE FROM sync_info WHERE type = 'following'")
        db.execSQL("INSERT INTO sync_info (type, last_sync_time) VALUES ('following', ?)", arrayOf<Any>(currentTime))

        db.close()
    }

    fun getCachedFollowing(): List<User> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT user_id, username, first_name, last_name, profile_image FROM cached_following", null)
        val following = mutableListOf<User>()

        while (cursor.moveToNext()) {
            following.add(
                User(
                    userId = cursor.getInt(0),
                    username = cursor.getString(1),
                    firstName = cursor.getString(2),
                    lastName = cursor.getString(3),
                    profileImage = cursor.getString(4)
                )
            )
        }

        cursor.close()
        db.close()
        return following
    }

    fun getLastSyncTime(type: String): Long {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT last_sync_time FROM sync_info WHERE type = ?", arrayOf(type))
        var syncTime = 0L

        if (cursor.moveToFirst()) {
            syncTime = cursor.getLong(0)
        }

        cursor.close()
        db.close()
        return syncTime
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