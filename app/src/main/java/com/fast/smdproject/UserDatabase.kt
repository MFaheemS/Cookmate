package com.fast.smdproject

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDatabase(context: Context) :
    SQLiteOpenHelper(context, "user_db", null, 9) { // Changed version to 9

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            "CREATE TABLE user (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "username TEXT," +
                    "first_name TEXT," +
                    "last_name TEXT," +
                    "email TEXT," +
                    "profile_image TEXT," +
                    "followers_count INTEGER DEFAULT 0," +
                    "following_count INTEGER DEFAULT 0," +
                    "uploads_count INTEGER DEFAULT 0," +
                    "is_private INTEGER DEFAULT 0," +
                    "allow_notifications INTEGER DEFAULT 1," +
                    "allow_recipe_notifications INTEGER DEFAULT 1," +
                    "logout_on_close INTEGER DEFAULT 0" +
                    ")"
        )

        createPendingTable(db)
        createFollowersTable(db)
        createFollowingTable(db)
        createSyncInfoTable(db)
        createDownloadedRecipesTable(db)
        createRemindersTable(db)
        createRecipeDetailsTable(db)
        createUserUploadsTable(db)
        createPendingActionsTable(db)
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

    private fun createDownloadedRecipesTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE downloaded_recipes (" +
                    "recipe_id INTEGER PRIMARY KEY," +
                    "user_id INTEGER," +
                    "title TEXT," +
                    "description TEXT," +
                    "tags TEXT," +
                    "images TEXT," +
                    "like_count INTEGER," +
                    "download_count INTEGER," +
                    "is_liked INTEGER," +
                    "is_downloaded INTEGER," +
                    "owner_id INTEGER," +
                    "downloaded_at TEXT," +
                    "synced INTEGER DEFAULT 1" +
                    ")"
        )
    }

    private fun createRemindersTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE reminders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "recipe_id INTEGER," +
                    "user_id INTEGER," +
                    "recipe_title TEXT," +
                    "reminder_time INTEGER," +
                    "image_path TEXT," +
                    "synced INTEGER DEFAULT 1," +
                    "UNIQUE(recipe_id, user_id)" +
                    ")"
        )
    }

    private fun createRecipeDetailsTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE recipe_details (" +
                    "recipe_id INTEGER PRIMARY KEY," +
                    "title TEXT," +
                    "description TEXT," +
                    "ingredients TEXT," +
                    "steps TEXT," +
                    "tags TEXT," +
                    "images TEXT," +
                    "like_count INTEGER," +
                    "download_count INTEGER," +
                    "is_liked INTEGER," +
                    "is_downloaded INTEGER," +
                    "owner_id INTEGER," +
                    "owner_username TEXT," +
                    "owner_profile_image TEXT," +
                    "cached_at INTEGER" +
                    ")"
        )
    }

    private fun createUserUploadsTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE user_uploads (" +
                    "recipe_id INTEGER PRIMARY KEY," +
                    "user_id INTEGER," +
                    "title TEXT," +
                    "description TEXT," +
                    "ingredients TEXT," +
                    "steps TEXT," +
                    "tags TEXT," +
                    "images TEXT," +
                    "like_count INTEGER," +
                    "download_count INTEGER," +
                    "created_at TEXT," +
                    "cached_at INTEGER" +
                    ")"
        )
    }

    private fun createPendingActionsTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE pending_actions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "action_type TEXT," +
                    "target_id INTEGER," +
                    "target_type TEXT," +
                    "additional_data TEXT," +
                    "created_at INTEGER," +
                    "attempts INTEGER DEFAULT 0" +
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
        if (oldV < 5) {
            createDownloadedRecipesTable(db)
            createRemindersTable(db)
        }
        if (oldV < 6) {
            createRecipeDetailsTable(db)
            createUserUploadsTable(db)
        }
        if (oldV < 7) {
            // Add follower/following/uploads counts to user table
            try {
                db.execSQL("ALTER TABLE user ADD COLUMN followers_count INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE user ADD COLUMN following_count INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE user ADD COLUMN uploads_count INTEGER DEFAULT 0")
            } catch (e: Exception) {
                // Columns might already exist
                e.printStackTrace()
            }
        }
        if (oldV < 8) {
            createPendingActionsTable(db)
        }
        if (oldV < 9) {
            // Add settings columns to user table
            try {
                db.execSQL("ALTER TABLE user ADD COLUMN is_private INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE user ADD COLUMN allow_notifications INTEGER DEFAULT 1")
                db.execSQL("ALTER TABLE user ADD COLUMN allow_recipe_notifications INTEGER DEFAULT 1")
                db.execSQL("ALTER TABLE user ADD COLUMN logout_on_close INTEGER DEFAULT 0")
            } catch (e: Exception) {
                // Columns might already exist
                e.printStackTrace()
            }
        }
    }


    fun saveUser(userId: Int, username: String, first: String, last: String, email: String, image: String?, followersCount: Int = 0, followingCount: Int = 0, uploadsCount: Int = 0) {
        val db = writableDatabase
        db.execSQL("DELETE FROM user")
        val profileImage = image ?: ""
        db.execSQL(
            "INSERT INTO user (user_id, username, first_name, last_name, email, profile_image, followers_count, following_count, uploads_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(userId, username, first, last, email, profileImage, followersCount, followingCount, uploadsCount)
        )
        db.close()
    }

    fun updateUserCounts(followersCount: Int, followingCount: Int, uploadsCount: Int) {
        val db = writableDatabase
        db.execSQL("UPDATE user SET followers_count = ?, following_count = ?, uploads_count = ?", arrayOf<Any>(followersCount, followingCount, uploadsCount))
        db.close()
    }

    fun getUserCounts(): Triple<Int, Int, Int> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT followers_count, following_count, uploads_count FROM user LIMIT 1", null)
        var counts = Triple(0, 0, 0)
        if (cursor.moveToFirst()) {
            counts = Triple(
                cursor.getInt(0),
                cursor.getInt(1),
                cursor.getInt(2)
            )
        }
        cursor.close()
        db.close()
        return counts
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

    // Downloaded Recipes methods
    fun saveDownloadedRecipe(recipe: Recipe, userId: Int, downloadedAt: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("recipe_id", recipe.recipeId)
            put("user_id", userId)
            put("title", recipe.title)
            put("description", recipe.description)
            put("tags", recipe.tags)
            put("images", recipe.imagePath)
            put("like_count", recipe.likeCount)
            put("download_count", recipe.downloadCount)
            put("is_liked", if (recipe.isLiked) 1 else 0)
            put("is_downloaded", if (recipe.isDownloaded) 1 else 0)
            put("owner_id", recipe.ownerId)
            put("downloaded_at", downloadedAt)
            put("synced", 1)
        }

        db.insertWithOnConflict("downloaded_recipes", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getDownloadedRecipes(userId: Int): Pair<List<Recipe>, Map<Int, String>> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM downloaded_recipes WHERE user_id = ? ORDER BY downloaded_at DESC",
            arrayOf(userId.toString())
        )

        val recipes = mutableListOf<Recipe>()
        val downloadTimes = mutableMapOf<Int, String>()

        while (cursor.moveToNext()) {
            val recipeId = cursor.getInt(cursor.getColumnIndexOrThrow("recipe_id"))
            val recipe = Recipe(
                recipeId = recipeId,
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                tags = cursor.getString(cursor.getColumnIndexOrThrow("tags")),
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow("images")),
                likeCount = cursor.getInt(cursor.getColumnIndexOrThrow("like_count")),
                downloadCount = cursor.getInt(cursor.getColumnIndexOrThrow("download_count")),
                isLiked = cursor.getInt(cursor.getColumnIndexOrThrow("is_liked")) == 1,
                isDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow("is_downloaded")) == 1,
                ownerId = cursor.getInt(cursor.getColumnIndexOrThrow("owner_id"))
            )
            recipes.add(recipe)
            downloadTimes[recipeId] = cursor.getString(cursor.getColumnIndexOrThrow("downloaded_at"))
        }

        cursor.close()
        db.close()
        return Pair(recipes, downloadTimes)
    }

    fun deleteDownloadedRecipe(recipeId: Int, userId: Int) {
        val db = writableDatabase
        db.delete("downloaded_recipes", "recipe_id = ? AND user_id = ?", arrayOf(recipeId.toString(), userId.toString()))
        db.close()
    }

    fun clearDownloadedRecipes(userId: Int) {
        val db = writableDatabase
        db.delete("downloaded_recipes", "user_id = ?", arrayOf(userId.toString()))
        db.close()
    }

    fun updateDownloadedRecipesSyncStatus(userId: Int) {
        val db = writableDatabase
        val currentTime = System.currentTimeMillis()
        db.execSQL("DELETE FROM sync_info WHERE type = 'downloaded_recipes'")
        db.execSQL("INSERT INTO sync_info (type, last_sync_time) VALUES ('downloaded_recipes', ?)", arrayOf<Any>(currentTime))
        db.close()
    }

    // Reminders methods
    fun saveReminder(reminder: Reminder, userId: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("recipe_id", reminder.recipeId)
            put("user_id", userId)
            put("recipe_title", reminder.recipeTitle)
            put("reminder_time", reminder.timeInMillis)
            put("image_path", reminder.imagePath)
            put("synced", 1)
        }

        db.insertWithOnConflict("reminders", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getReminders(userId: Int): List<Reminder> {
        val db = readableDatabase
        val currentTime = System.currentTimeMillis()
        val cursor = db.rawQuery(
            "SELECT DISTINCT recipe_id, recipe_title, reminder_time, image_path FROM reminders WHERE user_id = ? AND reminder_time > ? ORDER BY reminder_time ASC",
            arrayOf(userId.toString(), currentTime.toString())
        )

        val reminders = mutableListOf<Reminder>()
        val seenRecipeIds = mutableSetOf<Int>()

        while (cursor.moveToNext()) {
            val recipeId = cursor.getInt(cursor.getColumnIndexOrThrow("recipe_id"))
            // Ensure uniqueness by recipe_id
            if (!seenRecipeIds.contains(recipeId)) {
                seenRecipeIds.add(recipeId)
                val reminder = Reminder(
                    recipeId = recipeId,
                    recipeTitle = cursor.getString(cursor.getColumnIndexOrThrow("recipe_title")),
                    timeInMillis = cursor.getLong(cursor.getColumnIndexOrThrow("reminder_time")),
                    imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path")) ?: ""
                )
                reminders.add(reminder)
            }
        }

        cursor.close()
        db.close()
        return reminders
    }

    fun deleteReminder(recipeId: Int, userId: Int) {
        val db = writableDatabase
        db.delete("reminders", "recipe_id = ? AND user_id = ?", arrayOf(recipeId.toString(), userId.toString()))
        db.close()
    }

    fun deleteExpiredReminders(userId: Int) {
        val db = writableDatabase
        val currentTime = System.currentTimeMillis()
        db.delete("reminders", "user_id = ? AND reminder_time <= ?", arrayOf(userId.toString(), currentTime.toString()))
        db.close()
    }

    fun updateRemindersSyncStatus(userId: Int) {
        val db = writableDatabase
        val currentTime = System.currentTimeMillis()
        db.execSQL("DELETE FROM sync_info WHERE type = 'reminders'")
        db.execSQL("INSERT INTO sync_info (type, last_sync_time) VALUES ('reminders', ?)", arrayOf<Any>(currentTime))
        db.close()
    }

    // Recipe Details methods (full recipe data for offline viewing)
    fun saveRecipeDetail(
        recipeId: Int,
        title: String,
        description: String,
        ingredients: String,
        steps: String,
        tags: String,
        images: String,
        likeCount: Int,
        downloadCount: Int,
        isLiked: Boolean,
        isDownloaded: Boolean,
        ownerId: Int,
        ownerUsername: String,
        ownerProfileImage: String
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("recipe_id", recipeId)
            put("title", title)
            put("description", description)
            put("ingredients", ingredients)
            put("steps", steps)
            put("tags", tags)
            put("images", images)
            put("like_count", likeCount)
            put("download_count", downloadCount)
            put("is_liked", if (isLiked) 1 else 0)
            put("is_downloaded", if (isDownloaded) 1 else 0)
            put("owner_id", ownerId)
            put("owner_username", ownerUsername)
            put("owner_profile_image", ownerProfileImage)
            put("cached_at", System.currentTimeMillis())
        }

        db.insertWithOnConflict("recipe_details", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getRecipeDetail(recipeId: Int): HashMap<String, String>? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM recipe_details WHERE recipe_id = ?",
            arrayOf(recipeId.toString())
        )

        var recipeMap: HashMap<String, String>? = null

        if (cursor.moveToFirst()) {
            recipeMap = HashMap<String, String>().apply {
                put("recipe_id", cursor.getInt(cursor.getColumnIndexOrThrow("recipe_id")).toString())
                put("title", cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "")
                put("description", cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "")
                put("ingredients", cursor.getString(cursor.getColumnIndexOrThrow("ingredients")) ?: "")
                put("steps", cursor.getString(cursor.getColumnIndexOrThrow("steps")) ?: "")
                put("tags", cursor.getString(cursor.getColumnIndexOrThrow("tags")) ?: "")
                put("images", cursor.getString(cursor.getColumnIndexOrThrow("images")) ?: "")
                put("like_count", cursor.getInt(cursor.getColumnIndexOrThrow("like_count")).toString())
                put("download_count", cursor.getInt(cursor.getColumnIndexOrThrow("download_count")).toString())
                put("is_liked", cursor.getInt(cursor.getColumnIndexOrThrow("is_liked")).toString())
                put("is_downloaded", cursor.getInt(cursor.getColumnIndexOrThrow("is_downloaded")).toString())
                put("owner_id", cursor.getInt(cursor.getColumnIndexOrThrow("owner_id")).toString())
                put("owner_username", cursor.getString(cursor.getColumnIndexOrThrow("owner_username")) ?: "")
                put("owner_profile_image", cursor.getString(cursor.getColumnIndexOrThrow("owner_profile_image")) ?: "")
            }
        }

        cursor.close()
        db.close()
        return recipeMap
    }

    fun deleteRecipeDetail(recipeId: Int) {
        val db = writableDatabase
        db.delete("recipe_details", "recipe_id = ?", arrayOf(recipeId.toString()))
        db.close()
    }

    // User Uploads methods
    fun saveUserUpload(
        recipeId: Int,
        userId: Int,
        title: String,
        description: String,
        ingredients: String,
        steps: String,
        tags: String,
        images: String,
        likeCount: Int,
        downloadCount: Int,
        createdAt: String
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("recipe_id", recipeId)
            put("user_id", userId)
            put("title", title)
            put("description", description)
            put("ingredients", ingredients)
            put("steps", steps)
            put("tags", tags)
            put("images", images)
            put("like_count", likeCount)
            put("download_count", downloadCount)
            put("created_at", createdAt)
            put("cached_at", System.currentTimeMillis())
        }

        db.insertWithOnConflict("user_uploads", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getUserUploads(userId: Int): List<HashMap<String, String>> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM user_uploads WHERE user_id = ? ORDER BY cached_at DESC",
            arrayOf(userId.toString())
        )

        val uploads = mutableListOf<HashMap<String, String>>()

        while (cursor.moveToNext()) {
            val recipeMap = HashMap<String, String>().apply {
                put("recipe_id", cursor.getInt(cursor.getColumnIndexOrThrow("recipe_id")).toString())
                put("title", cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "")
                put("description", cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "")
                put("ingredients", cursor.getString(cursor.getColumnIndexOrThrow("ingredients")) ?: "")
                put("steps", cursor.getString(cursor.getColumnIndexOrThrow("steps")) ?: "")
                put("tags", cursor.getString(cursor.getColumnIndexOrThrow("tags")) ?: "")
                put("images", cursor.getString(cursor.getColumnIndexOrThrow("images")) ?: "")
                put("like_count", cursor.getInt(cursor.getColumnIndexOrThrow("like_count")).toString())
                put("download_count", cursor.getInt(cursor.getColumnIndexOrThrow("download_count")).toString())
                put("created_at", cursor.getString(cursor.getColumnIndexOrThrow("created_at")) ?: "")
            }
            uploads.add(recipeMap)
        }

        cursor.close()
        db.close()
        return uploads
    }

    fun clearUserUploads(userId: Int) {
        val db = writableDatabase
        db.delete("user_uploads", "user_id = ?", arrayOf(userId.toString()))
        db.close()
    }

    fun deleteUserUpload(recipeId: Int) {
        val db = writableDatabase
        db.delete("user_uploads", "recipe_id = ?", arrayOf(recipeId.toString()))
        db.close()
    }

    // Pending Actions Queue Methods
    fun queueAction(userId: Int, actionType: String, targetId: Int, targetType: String, additionalData: String = "") {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("action_type", actionType)
            put("target_id", targetId)
            put("target_type", targetType)
            put("additional_data", additionalData)
            put("created_at", System.currentTimeMillis())
            put("attempts", 0)
        }

        db.insert("pending_actions", null, values)
        db.close()
    }

    fun getPendingActions(userId: Int): List<HashMap<String, String>> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM pending_actions WHERE user_id = ? ORDER BY created_at ASC",
            arrayOf(userId.toString())
        )

        val actions = mutableListOf<HashMap<String, String>>()

        while (cursor.moveToNext()) {
            val actionMap = HashMap<String, String>().apply {
                put("id", cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString())
                put("action_type", cursor.getString(cursor.getColumnIndexOrThrow("action_type")) ?: "")
                put("target_id", cursor.getInt(cursor.getColumnIndexOrThrow("target_id")).toString())
                put("target_type", cursor.getString(cursor.getColumnIndexOrThrow("target_type")) ?: "")
                put("additional_data", cursor.getString(cursor.getColumnIndexOrThrow("additional_data")) ?: "")
                put("attempts", cursor.getInt(cursor.getColumnIndexOrThrow("attempts")).toString())
            }
            actions.add(actionMap)
        }

        cursor.close()
        db.close()
        return actions
    }

    fun deletePendingAction(actionId: Int) {
        val db = writableDatabase
        db.delete("pending_actions", "id = ?", arrayOf(actionId.toString()))
        db.close()
    }

    fun incrementActionAttempts(actionId: Int) {
        val db = writableDatabase
        db.execSQL("UPDATE pending_actions SET attempts = attempts + 1 WHERE id = ?", arrayOf<Any>(actionId))
        db.close()
    }

    fun clearOldPendingActions(userId: Int, olderThanDays: Int = 7) {
        val db = writableDatabase
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        db.delete("pending_actions", "user_id = ? AND created_at < ?", arrayOf(userId.toString(), cutoffTime.toString()))
        db.close()
    }

    // Settings Methods
    fun updatePrivateProfile(isPrivate: Boolean) {
        val db = writableDatabase
        db.execSQL("UPDATE user SET is_private = ?", arrayOf<Any>(if (isPrivate) 1 else 0))
        db.close()
    }

    fun isPrivateProfile(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT is_private FROM user LIMIT 1", null)
        var isPrivate = false
        if (cursor.moveToFirst()) {
            isPrivate = cursor.getInt(0) == 1
        }
        cursor.close()
        db.close()
        return isPrivate
    }

    fun updateAllowNotifications(allow: Boolean) {
        val db = writableDatabase
        db.execSQL("UPDATE user SET allow_notifications = ?", arrayOf<Any>(if (allow) 1 else 0))
        db.close()
    }

    fun allowNotifications(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT allow_notifications FROM user LIMIT 1", null)
        var allow = true
        if (cursor.moveToFirst()) {
            allow = cursor.getInt(0) == 1
        }
        cursor.close()
        db.close()
        return allow
    }

    fun updateAllowRecipeNotifications(allow: Boolean) {
        val db = writableDatabase
        db.execSQL("UPDATE user SET allow_recipe_notifications = ?", arrayOf<Any>(if (allow) 1 else 0))
        db.close()
    }

    fun allowRecipeNotifications(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT allow_recipe_notifications FROM user LIMIT 1", null)
        var allow = true
        if (cursor.moveToFirst()) {
            allow = cursor.getInt(0) == 1
        }
        cursor.close()
        db.close()
        return allow
    }

    fun updateLogoutOnClose(enabled: Boolean) {
        val db = writableDatabase
        db.execSQL("UPDATE user SET logout_on_close = ?", arrayOf<Any>(if (enabled) 1 else 0))
        db.close()
    }

    fun logoutOnClose(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT logout_on_close FROM user LIMIT 1", null)
        var enabled = false
        if (cursor.moveToFirst()) {
            enabled = cursor.getInt(0) == 1
        }
        cursor.close()
        db.close()
        return enabled
    }
}

