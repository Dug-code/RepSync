package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Friendship
import com.repsyncdemo.workout.data.model.FriendshipStatus
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.databinding.ItemUserSearchBinding

class UserSearchAdapter(
    private val currentUserId: String,
    private val onUserClick: (UserProfile) -> Unit,
    private val onAddFriend: (UserProfile) -> Unit,
    private val onCancelRequest: (String) -> Unit // Added callback for canceling
) : ListAdapter<UserProfile, UserSearchAdapter.ViewHolder>(UserDiffCallback()) {

    private var friendships: List<Friendship> = emptyList()

    fun updateFriendships(newList: List<Friendship>) {
        friendships = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserSearchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemUserSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserProfile) {
            binding.tvUsername.text = user.username
            
            // Load Profile Pic
            if (user.profilePictureUrl.isNotEmpty()) {
                binding.ivSearchProfilePic.load(user.profilePictureUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                    transformations(CircleCropTransformation())
                }
            } else {
                binding.ivSearchProfilePic.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Determine Friendship Status for Button
            val friendship = friendships.find { 
                (it.requesterId == currentUserId && it.receiverId == user.userId) ||
                (it.requesterId == user.userId && it.receiverId == currentUserId)
            }

            when (friendship?.status) {
                FriendshipStatus.ACCEPTED -> {
                    binding.btnAddFriend.text = "Friends"
                    binding.btnAddFriend.isEnabled = false
                    binding.btnAddFriend.alpha = 0.6f
                }
                FriendshipStatus.PENDING -> {
                    if (friendship.requesterId == currentUserId) {
                        binding.btnAddFriend.text = "Requested"
                        binding.btnAddFriend.isEnabled = true // Enable so it can be clicked to cancel
                        binding.btnAddFriend.alpha = 0.8f
                    } else {
                        binding.btnAddFriend.text = "Accept"
                        binding.btnAddFriend.isEnabled = true
                        binding.btnAddFriend.alpha = 1.0f
                    }
                }
                else -> {
                    binding.btnAddFriend.text = "Add Friend"
                    binding.btnAddFriend.isEnabled = true
                    binding.btnAddFriend.alpha = 1.0f
                }
            }

            binding.root.setOnClickListener { onUserClick(user) }
            
            binding.btnAddFriend.setOnClickListener { 
                when (binding.btnAddFriend.text) {
                    "Add Friend" -> onAddFriend(user)
                    "Requested" -> friendship?.let { onCancelRequest(it.id) }
                    "Accept" -> friendship?.let { /* In a full app, we might call an onAccept callback here too */ }
                }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(oldItem: UserProfile, newItem: UserProfile) = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: UserProfile, newItem: UserProfile) = oldItem == newItem
    }
}
