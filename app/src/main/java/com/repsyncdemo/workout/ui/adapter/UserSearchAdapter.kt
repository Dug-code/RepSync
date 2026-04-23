package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
    private val onCancelRequest: (String) -> Unit,
    private val onAcceptRequest: (Friendship) -> Unit = {}
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
            if (user.profilePictureUrl.isNotEmpty() && (user.profilePictureUrl.startsWith("http") || user.profilePictureUrl.startsWith("https"))) {
                binding.ivSearchProfilePic.load(user.profilePictureUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile_grey)
                    error(R.drawable.ic_profile_grey)
                    transformations(CircleCropTransformation())
                }
            } else {
                val resId = when(user.profilePictureUrl) {
                    "red" -> R.drawable.ic_profile_red
                    "blue" -> R.drawable.ic_profile_blue
                    "green" -> R.drawable.ic_profile_green
                    "yellow" -> R.drawable.ic_profile_yellow
                    "purple" -> R.drawable.ic_profile_purple
                    else -> R.drawable.ic_profile_grey
                }
                binding.ivSearchProfilePic.setImageResource(resId)
            }

            // Determine Friendship Status for Button
            val friendship = friendships.find { 
                (it.requesterId == currentUserId && it.receiverId == user.userId) ||
                (it.requesterId == user.userId && it.receiverId == currentUserId)
            }

            val btn = binding.btnAddFriend
            btn.alpha = 1.0f
            btn.isEnabled = true
            
            when (friendship?.status) {
                FriendshipStatus.ACCEPTED -> {
                    btn.text = "Remove"
                    btn.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.error))
                    btn.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                FriendshipStatus.PENDING -> {
                    if (friendship.requesterId == currentUserId) {
                        btn.text = "Requested"
                        btn.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                        btn.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                        btn.alpha = 0.8f
                    } else {
                        btn.text = "Accept"
                        btn.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.success))
                        btn.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                    }
                }
                else -> {
                    btn.text = "Add Friend"
                    btn.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.primary))
                    btn.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
            }

            binding.root.setOnClickListener { onUserClick(user) }
            
            btn.setOnClickListener { 
                when (btn.text) {
                    "Add Friend" -> onAddFriend(user)
                    "Requested" -> friendship?.let { onCancelRequest(it.id) }
                    "Accept" -> friendship?.let { onAcceptRequest(it) }
                    "Remove" -> friendship?.let { onCancelRequest(it.id) }
                }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(oldItem: UserProfile, newItem: UserProfile) = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: UserProfile, newItem: UserProfile) = oldItem == newItem
    }
}
