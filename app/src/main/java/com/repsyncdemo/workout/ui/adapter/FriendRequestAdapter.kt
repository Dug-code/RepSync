package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repsyncdemo.workout.data.model.Friendship
import com.repsyncdemo.workout.databinding.ItemFriendRequestBinding

class FriendRequestAdapter(
    private val onAccept: (Friendship) -> Unit,
    private val onDecline: (Friendship) -> Unit
) : ListAdapter<Friendship, FriendRequestAdapter.ViewHolder>(RequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFriendRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: Friendship) {
            binding.tvUsername.text = request.requesterUsername
            binding.btnAccept.setOnClickListener { onAccept(request) }
            binding.btnDecline.setOnClickListener { onDecline(request) }
        }
    }

    class RequestDiffCallback : DiffUtil.ItemCallback<Friendship>() {
        override fun areItemsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Friendship, newItem: Friendship) = oldItem == newItem
    }
}
