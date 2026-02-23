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
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import com.repsyncdemo.workout.databinding.ItemFeedPostBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedAdapter(
    private val onUserClick: (String) -> Unit,
    private val onLikeClick: (String) -> Unit,
    private val onDeleteClick: ((String) -> Unit)? = null
) : ListAdapter<FeedPost, FeedAdapter.ViewHolder>(FeedDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd 'at' h:mm a", Locale.getDefault())
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFeedPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: FeedPost) {
            binding.tvUsername.text = "@${post.username}"
            binding.tvUsername.setOnClickListener { onUserClick(post.userId) }
            binding.ivUserProfile.setOnClickListener { onUserClick(post.userId) }

            // Load profile picture
            if (post.userProfilePicture.isNotEmpty()) {
                binding.ivUserProfile.load(post.userProfilePicture) {
                    crossfade(true)
                    placeholder(android.graphics.drawable.ColorDrawable(0xFFEEEEEE.toInt()))
                    error(android.R.drawable.ic_menu_gallery)
                    transformations(CircleCropTransformation())
                }
            } else {
                binding.ivUserProfile.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            when (post.type) {
                FeedPostType.WORKOUT_SHARED -> {
                    binding.tvPostType.text = "Workout"
                    binding.tvPostContent.text = "Shared workout: ${post.workoutName}"
                }
                FeedPostType.GOAL_CREATED -> {
                    binding.tvPostType.text = "Goal"
                    binding.tvPostContent.text = "Set a new goal: ${post.goalTitle}"
                }
                FeedPostType.GOAL_COMPLETED -> {
                    binding.tvPostType.text = "Achievement"
                    binding.tvPostContent.text = "Completed goal: ${post.goalTitle}"
                }
                FeedPostType.PR_ACHIEVED -> {
                    binding.tvPostType.text = "PR"
                    binding.tvPostContent.text = "New PR: ${post.goalTitle}"
                }
                FeedPostType.CHAT_MESSAGE -> {
                    binding.tvPostType.text = "Chat"
                    binding.tvPostContent.text = post.description
                }
            }

            binding.tvDescription.text = if (post.type == FeedPostType.CHAT_MESSAGE) "" else post.description
            binding.tvTimestamp.text = dateFormat.format(Date(post.createdAt))

            // Likes
            binding.tvLikeCount.text = post.likes.size.toString()
            val isLiked = post.likes.contains(currentUserId)
            binding.ivLike.setImageResource(
                if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            )
            binding.btnLikeArea.setOnClickListener { onLikeClick(post.id) }

            // Delete button visibility
            if (post.userId == currentUserId && onDeleteClick != null) {
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnDelete.setOnClickListener { onDeleteClick.invoke(post.id) }
            } else {
                binding.btnDelete.visibility = View.GONE
            }
        }
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<FeedPost>() {
        override fun areItemsTheSame(oldItem: FeedPost, newItem: FeedPost) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FeedPost, newItem: FeedPost) = oldItem == newItem
    }
}
