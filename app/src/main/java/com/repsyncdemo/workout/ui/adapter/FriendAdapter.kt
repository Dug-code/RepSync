package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.data.model.Friendship
import com.repsyncdemo.workout.databinding.ItemFriendBinding

class FriendAdapter(
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
            binding.tvUsername.text = if (friendship.requesterId == currentUserId) {
                friendship.receiverUsername
            } else {
                friendship.requesterUsername
            }
            binding.btnAction.setOnClickListener { onRemove(friendship) }
        }
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<Friendship>() {
        override fun areItemsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem == newItem
    }
}
