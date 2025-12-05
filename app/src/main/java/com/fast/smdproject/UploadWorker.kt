package com.fast.smdproject

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class UploadWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext
        val db = UserDatabase(context)

        // 1. Get all pending recipes
        val pendingList = db.getPendingRecipes()

        if (pendingList.isEmpty()) {
            return Result.success()
        }

        val ipAddress = context.getString(R.string.ipAddress)
        val urlString = "http://$ipAddress/cookMate/uploadRecipe.php"

        var allSuccess = true // Tracker for batch success

        // 2. Loop and Upload
        for (recipe in pendingList) {
            val success = uploadSync(urlString, recipe)

            if (success) {
                // Remove from local DB immediately if upload succeeded
                db.deletePendingRecipe(recipe["id"]!!)
            } else {
                // If one fails, mark flag as false but CONTINUE the loop
                // so other recipes still get a chance to upload.
                allSuccess = false
            }
        }

        // 3. Determine Final Result
        // If even one failed, we tell WorkManager to retry later
        return if (allSuccess) Result.success() else Result.retry()
    }

    // Synchronous Upload Function (without Volley, because Workers run on bg thread)
    private fun uploadSync(urlString: String, params: HashMap<String, String>): Boolean {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.doInput = true

            // Prepare POST data
            val postData = StringBuilder()
            postData.append(URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(params["username"], "UTF-8") + "&")
            postData.append(URLEncoder.encode("unique_id", "UTF-8") + "=" + URLEncoder.encode(params["unique_id"], "UTF-8") + "&")
            postData.append(URLEncoder.encode("title", "UTF-8") + "=" + URLEncoder.encode(params["title"], "UTF-8") + "&")
            postData.append(URLEncoder.encode("description", "UTF-8") + "=" + URLEncoder.encode(params["description"], "UTF-8") + "&")
            postData.append(URLEncoder.encode("ingredients", "UTF-8") + "=" + URLEncoder.encode(params["ingredients"], "UTF-8") + "&")
            postData.append(URLEncoder.encode("steps", "UTF-8") + "=" + URLEncoder.encode(params["steps"], "UTF-8") + "&")
            postData.append(URLEncoder.encode("tags", "UTF-8") + "=" + URLEncoder.encode(params["tags"], "UTF-8") + "&")

            // Image might be empty
            val img = params["image_base64"] ?: ""
            postData.append(URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(img, "UTF-8"))

            // Send Data
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(postData.toString())
            writer.flush()
            writer.close()

            // Check Response Code
            val responseCode = conn.responseCode
            return responseCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}