package com.fast.smdproject

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.*

class SetTimerActivity : AppCompatActivity() {

    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var amPmPicker: NumberPicker
    private lateinit var btnConfirm: Button
    private lateinit var btnClose: ImageView

    private var recipeId: Int = 0
    private var recipeTitle: String = ""
    private var recipeImage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_timer)

        // Get recipe details from intent
        recipeId = intent.getIntExtra("RECIPE_ID", 0)
        recipeTitle = intent.getStringExtra("RECIPE_TITLE") ?: ""
        recipeImage = intent.getStringExtra("RECIPE_IMAGE") ?: ""

        // Initialize views
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        amPmPicker = findViewById(R.id.amPmPicker)
        btnConfirm = findViewById(R.id.btn_confirm_timer)
        btnClose = findViewById(R.id.btnCloseTimer)

        // Setup Hour Picker (1-12)
        hourPicker.minValue = 1
        hourPicker.maxValue = 12
        hourPicker.wrapSelectorWheel = true
        hourPicker.value = Calendar.getInstance().get(Calendar.HOUR).let { if (it == 0) 12 else it }

        // Setup Minute Picker (0-59)
        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        minutePicker.wrapSelectorWheel = true
        minutePicker.value = Calendar.getInstance().get(Calendar.MINUTE)
        minutePicker.setFormatter { value -> String.format(Locale.getDefault(), "%02d", value) }

        // Setup AM/PM Picker
        amPmPicker.minValue = 0
        amPmPicker.maxValue = 1
        amPmPicker.displayedValues = arrayOf("AM", "PM")
        amPmPicker.wrapSelectorWheel = true
        amPmPicker.value = if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) 0 else 1

        // Make pickers smooth and responsive
        setupPickerBehavior(hourPicker)
        setupPickerBehavior(minutePicker)
        setupPickerBehavior(amPmPicker)

        // Close button click - cancel and go back
        btnClose.setOnClickListener {
            AnimationUtils.buttonPressEffect(it) {
                finish()
            }
        }

        // Confirm button click
        btnConfirm.setOnClickListener {
            com.fast.smdproject.AnimationUtils.buttonPressEffect(it) {
                confirmTimer()
            }
        }
    }

    private fun setupPickerBehavior(picker: NumberPicker) {
        // Enable smooth scrolling
        picker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    }

    private fun confirmTimer() {
        val selectedHour = hourPicker.value
        val selectedMinute = minutePicker.value
        val isAM = amPmPicker.value == 0

        // Convert to 24-hour format
        val hour24 = when {
            isAM && selectedHour == 12 -> 0
            !isAM && selectedHour != 12 -> selectedHour + 12
            else -> selectedHour
        }

        // Calculate the target time
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour24)
        calendar.set(Calendar.MINUTE, selectedMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If the time is in the past, set it for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Schedule the alarm/notification
        scheduleNotification(calendar.timeInMillis)

        // Save reminder to database or preferences
        saveReminder(calendar.timeInMillis)

        // Show confirmation
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d %s", selectedHour, selectedMinute, if (isAM) "AM" else "PM")
        Toast.makeText(this, "Timer set for $timeStr for $recipeTitle", Toast.LENGTH_LONG).show()

        finish()
    }

    private fun scheduleNotification(timeInMillis: Long) {
        val alarmManager = getSystemService(AlarmManager::class.java)
        val intent = Intent(this, ReminderReceiver::class.java)
        intent.putExtra("RECIPE_ID", recipeId)
        intent.putExtra("RECIPE_TITLE", recipeTitle)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            recipeId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Fallback if exact alarm permission is not granted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }

    private fun saveReminder(timeInMillis: Long) {
        val db = UserDatabase(this)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // Save to local SQLite database first (works offline)
        val reminder = Reminder(recipeId, recipeTitle, timeInMillis, recipeImage)
        db.saveReminder(reminder, userId)

        // Then sync with server (only when online)
        syncReminderToDatabase(timeInMillis, userId, db)
    }

    private fun syncReminderToDatabase(timeInMillis: Long, userId: Int, db: UserDatabase) {
        val ipAddress = getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/save_reminder.php"

        val request = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        // Successfully synced to server
                        db.updateRemindersSyncStatus(userId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                // Failed to sync, but local SQLite storage still works
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId.toString()
                params["recipe_id"] = recipeId.toString()
                params["reminder_time"] = timeInMillis.toString()
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}

