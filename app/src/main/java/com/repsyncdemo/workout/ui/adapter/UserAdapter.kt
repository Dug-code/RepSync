package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.databinding.ItemUserAdminBinding

/**
 * Adapter used in the Admin Dashboard to list and manage users.
 * Provides toggles for Admin and Moderator permissions.
 */
class UserAdapter(
    private val onToggleAdmin: (UserProfile) -> Unit,
    private val onToggleModerator: (UserProfile) -> Unit
) : ListAdapter<UserProfile, UserAdapter.ViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserAdminBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemUserAdminBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * Binds a user profile to the item view, setting up UI state and click listeners.
         */
        fun bind(user: UserProfile) {
            binding.tvUsername.text = user.username
            binding.tvEmail.text = user.email
            
            // Display profile picture with circular crop
            val url = user.profilePictureUrl
            if (url.isNotEmpty() && (url.startsWith("http") || url.startsWith("https"))) {
                binding.ivUserProfile.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile_red)
                    error(R.drawable.ic_profile_red)
                    transformations(CircleCropTransformation())
                }
            } else {
                val resId = when(url) {
                    "red" -> R.drawable.ic_profile_red
                    "blue" -> R.drawable.ic_profile_blue
                    "green" -> R.drawable.ic_profile_green
                    "yellow" -> R.drawable.ic_profile_yellow
                    "purple" -> R.drawable.ic_profile_purple
                    else -> R.drawable.ic_profile_grey
                }
                binding.ivUserProfile.setImageResource(resId)
            }

            // Set current toggle states based on account flags
            binding.switchAdmin.isChecked = user.isAdmin
            binding.switchModerator.isChecked = user.isModerator

            // Handle permission changes
            binding.switchAdmin.setOnClickListener { onToggleAdmin(user) }
            binding.switchModerator.setOnClickListener { onToggleModerator(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(oldItem: UserProfile, newItem: UserProfile) = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: UserProfile, newItem: UserProfile) = oldItem == newItem
    }
}
