package com.fast.smdproject

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SetTimerActivity : AppCompatActivity() {

    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var amPmPicker: NumberPicker
    private lateinit var btnConfirm: Button

    private var recipeId: Int = 0
    private var recipeTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_timer)

        // Get recipe details from intent
        recipeId = intent.getIntExtra("RECIPE_ID", 0)
        recipeTitle = intent.getStringExtra("RECIPE_TITLE") ?: ""

        // Initialize views
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        amPmPicker = findViewById(R.id.amPmPicker)
        btnConfirm = findViewById(R.id.btn_confirm_timer)

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

        // Confirm button click
        btnConfirm.setOnClickListener {
            confirmTimer()
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
        val sharedPrefs = getSharedPreferences("Reminders", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Store reminder with recipe ID as key
        editor.putLong("reminder_$recipeId", timeInMillis)
        editor.putString("reminder_title_$recipeId", recipeTitle)
        editor.apply()
    }
}

