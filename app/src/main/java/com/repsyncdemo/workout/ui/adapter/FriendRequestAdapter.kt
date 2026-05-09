package com.repsyncdemo.workout.ui.adapter

/**
 * File overview: Binds pending friend requests with accept, decline, and profile navigation actions.
 */

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.Friendship
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.databinding.ItemFriendRequestBinding

class FriendRequestAdapter(
    private val onAccept: (Friendship) -> Unit,
    private val onDecline: (Friendship) -> Unit,
    private val onUserClick: (String) -> Unit
) : ListAdapter<Friendship, FriendRequestAdapter.ViewHolder>(RequestDiffCallback()) {

    private var profileMap = mapOf<String, UserProfile>()

    // Updates data or UI state.
    fun updateProfiles(profiles: Map<String, UserProfile>) {
        this.profileMap = profiles
        notifyDataSetChanged()
    }

    // Creates the item row.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    // Shows the item row.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFriendRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Fills this row with data.
        fun bind(request: Friendship) {
            val requesterId = request.requesterId
            val profile = profileMap[requesterId]
            
            val username = profile?.username ?: request.requesterUsername
            val profilePic = profile?.profilePictureUrl ?: "red"

            binding.tvUsername.text = "@$username"
            
            // Load latest profile picture for the requester
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

            binding.btnAccept.setOnClickListener { onAccept(request) }
            binding.btnDecline.setOnClickListener { onDecline(request) }
            binding.root.setOnClickListener { onUserClick(requesterId) }
        }
    }

    class RequestDiffCallback : DiffUtil.ItemCallback<Friendship>() {
        override fun areItemsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem == newItem
    }
}
