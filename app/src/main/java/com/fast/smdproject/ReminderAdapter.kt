package com.fast.smdproject

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ReminderAdapter(
    private val context: Context,
    private val reminders: MutableList<Reminder>,
    private val ipAddress: String,
    private val onReminderDeleted: () -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRecipe: ImageView = itemView.findViewById(R.id.img_reminder_recipe)
        val txtRecipeName: TextView = itemView.findViewById(R.id.tv_reminder_recipe_name)
        val txtTime: TextView = itemView.findViewById(R.id.tv_reminder_time)
        val txtTimeLeft: TextView = itemView.findViewById(R.id.tv_time_left)
        val btnEdit: Button = itemView.findViewById(R.id.btn_edit_reminder)
        val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_reminder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]

        holder.txtRecipeName.text = reminder.recipeTitle

        // Format time
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = reminder.timeInMillis
        val timeFormat = SimpleDateFormat("hh:mm . a", Locale.getDefault())
        holder.txtTime.text = timeFormat.format(calendar.time)

        // Calculate time left
        val timeLeft = reminder.timeInMillis - System.currentTimeMillis()
        holder.txtTimeLeft.text = formatTimeLeft(timeLeft)

        // Load recipe image
        if (reminder.imagePath.isNotEmpty()) {
            val fullImageUrl = "http://$ipAddress/cookMate/${reminder.imagePath}"
            Glide.with(context)
                .load(fullImageUrl)
                .placeholder(R.drawable.logo2)
                .error(R.drawable.logo2)
                .into(holder.imgRecipe)
        } else {
            holder.imgRecipe.setImageResource(R.drawable.logo2)
        }

        // Edit button - opens SetTimerActivity to edit the reminder
        holder.btnEdit.setOnClickListener {
            val intent = Intent(context, SetTimerActivity::class.java)
            intent.putExtra("RECIPE_ID", reminder.recipeId)
            intent.putExtra("RECIPE_TITLE", reminder.recipeTitle)
            intent.putExtra("EDIT_MODE", true)
            intent.putExtra("EXISTING_TIME", reminder.timeInMillis)
            context.startActivity(intent)
        }

        // Delete button
        holder.btnDelete.setOnClickListener {
            deleteReminder(reminder, position)
        }
    }

    private fun formatTimeLeft(milliseconds: Long): String {
        if (milliseconds < 0) {
            return "Expired"
        }

        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60

        return when {
            hours > 24 -> {
                val days = hours / 24
                "$days day${if (days > 1) "s" else ""} left"
            }
            hours > 0 -> {
                if (minutes > 0) {
                    "$hours hr $minutes min left"
                } else {
                    "$hours hr left"
                }
            }
            minutes > 0 -> "$minutes min left"
            else -> "Less than a minute"
        }
    }

    private fun deleteReminder(reminder: Reminder, position: Int) {
        // Cancel the alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.recipeId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Remove from SharedPreferences
        val sharedPrefs = context.getSharedPreferences("Reminders", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.remove("reminder_${reminder.recipeId}")
        editor.remove("reminder_title_${reminder.recipeId}")
        editor.remove("reminder_image_${reminder.recipeId}")
        editor.apply()

        // Remove from list and update UI
        reminders.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, reminders.size)

        Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show()

        // Sync deletion with database
        deleteFromDatabase(reminder.recipeId)

        // Callback to parent activity
        onReminderDeleted()
    }

    private fun deleteFromDatabase(recipeId: Int) {
        val db = UserDatabase(context)
        val userId = db.getCurrentUserId()

        if (userId == 0) {
            return
        }

        val ipAddress = context.getString(R.string.ipAddress)
        val url = "http://$ipAddress/cookMate/delete_reminder.php"

        val request = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    // Successfully deleted from database
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                // Failed to delete from database, but local deletion still works
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId.toString()
                params["recipe_id"] = recipeId.toString()
                return params
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    override fun getItemCount(): Int = reminders.size
}

