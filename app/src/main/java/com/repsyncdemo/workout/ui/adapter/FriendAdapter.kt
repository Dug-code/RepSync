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
import com.repsyncdemo.workout.databinding.ItemFriendBinding

class FriendAdapter(
    private val isMyProfile: Boolean,
    private val onRemove: (Friendship) -> Unit
) : ListAdapter<Friendship, FriendAdapter.ViewHolder>(FriendDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

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
            val isRequester = friendship.requesterId == currentUserId
            
            // Logic to determine which user in the friendship is the "friend" relative to currentUserId
            // or relative to the profile owner being viewed.
            // For simplicity, we show the username that is NOT the profile owner if we can identify it.
            
            binding.tvUsername.text = if (isRequester) {
                friendship.receiverUsername
            } else {
                friendship.requesterUsername
            }

            // Load default icon (placeholder) - in a full implementation, you'd fetch the actual profile here
            binding.ivProfilePic.setImageResource(R.drawable.ic_profile_red)

            if (isMyProfile) {
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.setOnClickListener { onRemove(friendship) }
            } else {
                binding.btnAction.visibility = View.GONE
            }
        }
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<Friendship>() {
        override fun areItemsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem == newItem
    }
}
