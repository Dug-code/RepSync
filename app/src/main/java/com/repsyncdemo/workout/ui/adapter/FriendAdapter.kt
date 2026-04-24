package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Friendship
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.databinding.ItemFriendBinding

class FriendAdapter(
    private val isMyProfile: Boolean,
    private val onRemove: (Friendship) -> Unit,
    private val onUserClick: (String) -> Unit
) : ListAdapter<Friendship, FriendAdapter.ViewHolder>(FriendDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var profileMap = mapOf<String, UserProfile>()

    fun updateProfiles(profiles: Map<String, UserProfile>) {
        this.profileMap = profiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFriendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friendship: Friendship) {
            val otherUserId = if (friendship.requesterId == currentUserId) friendship.receiverId else friendship.requesterId
            val profile = profileMap[otherUserId]

            // Display latest data from profile if available, fallback to friendship data
            val username = profile?.username ?: if (friendship.requesterId == currentUserId) friendship.receiverUsername else friendship.requesterUsername
            val profilePic = profile?.profilePictureUrl ?: "red"

            binding.tvUsername.text = "@$username"
            binding.ivAdminBadge.visibility = if (profile?.isAdmin == true) View.VISIBLE else View.GONE
            
            // Load latest profile picture
            if (profilePic.startsWith("http")) {
                binding.ivProfilePic.load(profilePic) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            } else {
                val resId = when(profilePic) {
                    "red" -> R.drawable.ic_profile_red
                    "blue" -> R.drawable.ic_profile_blue
                    "green" -> R.drawable.ic_profile_green
                    "yellow" -> R.drawable.ic_profile_yellow
                    "purple" -> R.drawable.ic_profile_purple
                    else -> R.drawable.ic_profile_grey
                }
                binding.ivProfilePic.setImageResource(resId)
            }

            if (isMyProfile) {
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.setOnClickListener { onRemove(friendship) }
            } else {
                binding.btnAction.visibility = View.GONE
            }

            binding.root.setOnClickListener { onUserClick(otherUserId) }
        }
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<Friendship>() {
        override fun areItemsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem == newItem
    }
}
