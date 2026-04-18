package com.repsyncdemo.workout.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.repsyncdemo.workout.R
import com.repsyncdemo.workout.data.model.FeedPost
import com.repsyncdemo.workout.data.model.FeedPostType
import com.repsyncdemo.workout.data.model.UserProfile
import com.repsyncdemo.workout.databinding.ItemFeedPostBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedAdapter(
    private val onUserClick: (String) -> Unit,
    private val onLikeClick: (String) -> Unit,
    //add onReactionClick to handle the call back from the feed fragment
    private val onReactionClick: (View, String) -> Unit,
    private val onDeleteClick: (String) -> Unit,
    private val onEditChatClick: ((FeedPost) -> Unit)? = null
) : ListAdapter<FeedPost, FeedAdapter.ViewHolder>(FeedDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd 'at' h:mm a", Locale.getDefault())
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    private var userProfiles: Map<String, UserProfile> = mutableMapOf()

    fun updateProfiles(profiles: Map<String, UserProfile>) {
        val merged = userProfiles.toMutableMap()
        merged.putAll(profiles)
        this.userProfiles = merged
        notifyDataSetChanged()
    }

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
            val latestProfile = userProfiles[post.userId]
            val displayUsername = latestProfile?.username ?: post.username
            val displayProfilePic = latestProfile?.profilePictureUrl ?: post.userProfilePicture

            binding.tvUsername.text = "@$displayUsername"
            binding.tvUsername.setOnClickListener { onUserClick(post.userId) }
            binding.ivUserProfile.setOnClickListener { onUserClick(post.userId) }

            loadProfilePicture(displayProfilePic)

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

            binding.tvLikeCount.text = post.likes.size.toString()
            val isLiked = post.likes.contains(currentUserId)
            binding.ivLike.setImageResource(
                if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            )
            binding.btnLikeArea.setOnClickListener { onLikeClick(post.id) }

            binding.tvReactionCount.text = post.reactions.size.toString()
            
            // Display first 3 unique reactions
            val recentEmojis = post.reactions.values.distinct().take(3)
            if (recentEmojis.isNotEmpty()) {
                binding.llRecentReactions.visibility = View.VISIBLE
                binding.tvReaction1.text = recentEmojis.getOrNull(0) ?: ""
                binding.tvReaction2.text = recentEmojis.getOrNull(1) ?: ""
                binding.tvReaction3.text = recentEmojis.getOrNull(2) ?: ""
                
                binding.tvReaction1.visibility = if (recentEmojis.size >= 1) View.VISIBLE else View.GONE
                binding.tvReaction2.visibility = if (recentEmojis.size >= 2) View.VISIBLE else View.GONE
                binding.tvReaction3.visibility = if (recentEmojis.size >= 3) View.VISIBLE else View.GONE
            } else {
                binding.llRecentReactions.visibility = View.GONE
            }
            binding.btnReactionArea.setOnClickListener { onReactionClick(it, post.id) }




            // Handle Long Press for Delete/Edit
            binding.postRoot.setOnLongClickListener {
                if (post.userId == currentUserId) {
                    showPostOptions(post)
                }
                true
            }
        }

        private fun showPostOptions(post: FeedPost) {
            val options = if (post.type == FeedPostType.CHAT_MESSAGE) {
                arrayOf("Edit Chat", "Delete Post")
            } else {
                arrayOf("Delete Post")
            }

            MaterialAlertDialogBuilder(binding.root.context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Post Options")
                .setItems(options) { _, which ->
                    when (options[which]) {
                        "Delete Post" -> {
                            MaterialAlertDialogBuilder(binding.root.context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                                .setTitle("Delete Post")
                                .setMessage("Are you sure you want to delete this post?")
                                .setPositiveButton("Delete") { _, _ -> onDeleteClick(post.id) }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                        "Edit Chat" -> {
                            onEditChatClick?.invoke(post)
                        }
                    }
                }
                .show()
        }

        private fun loadProfilePicture(url: String) {
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
                    "grey" -> R.drawable.ic_profile_grey
                    else -> R.drawable.ic_profile_red
                }
                binding.ivUserProfile.setImageResource(resId)
            }
        }
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<FeedPost>() {
        override fun areItemsTheSame(oldItem: FeedPost, newItem: FeedPost) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FeedPost, newItem: FeedPost) = oldItem == newItem
    }
}
