package com.repsyncdemo.workout.ui.adapter

/**
 * File overview: Binds friend rows and profile navigation for a viewed user's friends list.
 */

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
    private val profileOwnerId: String, // Added to correctly identify the "friend"
    private val onRemove: (Friendship) -> Unit,
    private val onUserClick: (String) -> Unit
) : ListAdapter<Friendship, FriendAdapter.ViewHolder>(FriendDiffCallback()) {

    private var profileMap = mapOf<String, UserProfile>()

    // Updates data or UI state.
    fun updateProfiles(profiles: Map<String, UserProfile>) {
        this.profileMap = profiles
        notifyDataSetChanged()
    }

    // Creates the item row.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    // Shows the item row.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFriendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Fills this row with data.
        fun bind(friendship: Friendship) {
            // The "friend" is whichever ID is NOT the owner of the profile we are viewing
            val friendId = if (friendship.requesterId == profileOwnerId) friendship.receiverId else friendship.requesterId
            val profile = profileMap[friendId]

            val username = profile?.username ?: if (friendship.requesterId == profileOwnerId) friendship.receiverUsername else friendship.requesterUsername
            val profilePic = profile?.profilePictureUrl ?: "red"

            binding.tvUsername.text = "@$username"
            binding.ivAdminBadge.visibility = if (profile?.isAdmin == true) View.VISIBLE else View.GONE
            
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

            binding.root.setOnClickListener { onUserClick(friendId) }
        }
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<Friendship>() {
        override fun areItemsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem == newItem
    }
}
