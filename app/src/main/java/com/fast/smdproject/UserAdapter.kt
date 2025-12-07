package com.fast.smdproject

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserAdapter(
    private val context: Context,
    private val userList: List<User>,
    private val ipAddress: String
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userAvatar: ImageView = itemView.findViewById(R.id.userAvatar)
        val userName: TextView = itemView.findViewById(R.id.userName)
        val userFullName: TextView = itemView.findViewById(R.id.userFullName)
        val btnFollow: Button = itemView.findViewById(R.id.btnFollow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.userName.text = "@${user.username}"
        holder.userFullName.text = "${user.firstName} ${user.lastName}"

        // Add slide-up fade-in animation with stagger
        com.fast.smdproject.AnimationUtils.slideUpFadeIn(holder.itemView, position * 30L)

        // Load profile image if available
        if (!user.profileImage.isNullOrEmpty()) {
            val imageUrl = "http://$ipAddress/cookMate/${user.profileImage}"
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(holder.userAvatar)
        } else {
            holder.userAvatar.setImageResource(R.drawable.default_avatar)
        }

        // Check if profile is private
        if (user.isPrivate) {
            // Grey out the button for private profiles
            holder.btnFollow.alpha = 0.5f
            holder.btnFollow.text = "Private"
            holder.btnFollow.isEnabled = false
            holder.itemView.alpha = 0.7f
        } else {
            // Normal button for public profiles
            holder.btnFollow.alpha = 1.0f
            holder.btnFollow.text = "View"
            holder.btnFollow.isEnabled = true
            holder.itemView.alpha = 1.0f
        }

        // Click on entire item or button to view profile with animations
        val clickListener = View.OnClickListener { view ->
            if (!user.isPrivate) {
                com.fast.smdproject.AnimationUtils.buttonPressEffect(view) {
                    val intent = Intent(context, SecondUserProfileActivity::class.java)
                    intent.putExtra("user_id", user.userId)
                    intent.putExtra("username", user.username)
                    context.startActivity(intent)
                    (context as? android.app.Activity)?.overridePendingTransition(
                        R.anim.activity_enter,
                        R.anim.activity_exit
                    )
                }
            } else {
                // Show message for private profile
                android.widget.Toast.makeText(context, "This profile is private", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        holder.itemView.setOnClickListener(clickListener)
        holder.btnFollow.setOnClickListener(clickListener)
    }

    override fun getItemCount(): Int = userList.size
}

