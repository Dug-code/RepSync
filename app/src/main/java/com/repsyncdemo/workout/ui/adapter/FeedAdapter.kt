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

/**
 * Adapter for the main social feed. Handles different post types, reactions,
 * and moderator capabilities.
 */
class FeedAdapter(
    private val onUserClick: (String) -> Unit,
    private val onReactionClick: (View, String) -> Unit,
    private val onDeleteClick: (String) -> Unit,
    private val onEditChatClick: ((FeedPost) -> Unit)? = null
) : ListAdapter<FeedPost, FeedAdapter.ViewHolder>(FeedDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd 'at' h:mm a", Locale.getDefault())
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    //Cache for user profiles to avoid repeated Firestore lookups for post headers
    private var userProfiles: Map<String, UserProfile> = mutableMapOf()
    //Current user's profile to determine moderation permissions
    private var currentUserProfile: UserProfile? = null

    /**
     * Updates the local profile cache with new data from the ViewModel.
     */
    fun updateProfiles(profiles: Map<String, UserProfile>) {
        val merged = userProfiles.toMutableMap()
        merged.putAll(profiles)
        this.userProfiles = merged
        notifyDataSetChanged()
    }

    /**
     * Sets the logged-in user's profile to enable role-based UI features (like moderation).
     */
    fun setCurrentUserProfile(profile: UserProfile?) {
        this.currentUserProfile = profile
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

        /**
         * Binds post data to the UI, including user info, content, and reaction counts.
         */
        fun bind(post: FeedPost) {
            val latestProfile = userProfiles[post.userId]
            val displayUsername = latestProfile?.username ?: post.username
            val displayProfilePic = latestProfile?.profilePictureUrl ?: post.userProfilePicture

            binding.tvUsername.text = "@$displayUsername"
            binding.tvUsername.setOnClickListener { onUserClick(post.userId) }
            binding.ivUserProfile.setOnClickListener { onUserClick(post.userId) }

            loadProfilePicture(displayProfilePic)

            // Customize display text based on post type
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

            // Display distance away if location services are active for this post
            post.distanceMiles?.let { distance ->
                binding.tvDistanceAway.text = String.format(Locale.getDefault(), "• %.1f mi away", distance)
                binding.tvDistanceAway.visibility = View.VISIBLE
            } ?: run {
                binding.tvDistanceAway.visibility = View.GONE
            }

            // Reaction Logic: Group identical reactions and show counts (e.g., "🔥 3")
            val reactionCounts = post.reactions.values.groupingBy { it }.eachCount()
            val uniqueReactions = reactionCounts.keys.toList().take(3)
            
            if (uniqueReactions.isNotEmpty()) {
                binding.llRecentReactions.visibility = View.VISIBLE
                binding.tvReactionCount.text = post.reactions.size.toString()
                
                val views = listOf(binding.tvReaction1, binding.tvReaction2, binding.tvReaction3)
                views.forEach { it.visibility = View.GONE }
                
                uniqueReactions.forEachIndexed { index, emoji ->
                    val count = reactionCounts[emoji] ?: 0
                    val display = if (count > 1) "$emoji$count" else emoji
                    views[index].apply {
                        text = display
                        visibility = View.VISIBLE
                    }
                }
            } else {
                binding.llRecentReactions.visibility = View.GONE
                binding.tvReactionCount.text = "React"
            }
            
            binding.btnReactionArea.setOnClickListener { onReactionClick(it, post.id) }

            // MODERATION: Check if current user has staff permissions
            val canModerate = currentUserProfile?.isAdmin == true || currentUserProfile?.isModerator == true
            binding.postRoot.setOnLongClickListener {
                // Allow long-press options if it's the user's own post OR if they are a moderator
                if (post.userId == currentUserId || canModerate) {
                    showPostOptions(post, canModerate && post.userId != currentUserId)
                }
                true
            }
        }

        /**
         * Shows context menu for a post (Edit/Delete).
         * @param isModerating If true, the user is deleting someone else's post via staff perms.
         */
        private fun showPostOptions(post: FeedPost, isModerating: Boolean) {
            val options = mutableListOf<String>()
            
            // Only owners can edit chat messages
            if (post.type == FeedPostType.CHAT_MESSAGE && post.userId == currentUserId) {
                options.add("Edit Chat")
            }
            
            // Distinguish delete action for transparency
            val deleteLabel = if (isModerating) "Delete Post (Moderator)" else "Delete Post"
            options.add(deleteLabel)

            MaterialAlertDialogBuilder(binding.root.context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle(if (isModerating) "Moderation Options" else "Post Options")
                .setItems(options.toTypedArray()) { _, which ->
                    when (options[which]) {
                        deleteLabel -> {
                            MaterialAlertDialogBuilder(binding.root.context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                                .setTitle("Delete Post")
                                .setMessage(if (isModerating) "As a moderator, are you sure you want to delete this user's post?" else "Are you sure you want to delete this post?")
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
            // Clear the image to prevent old profile pictures from showing during recycle
            binding.ivUserProfile.setImageDrawable(null)

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
        override fun areContentsTheSame(oldItem: FeedPost, newItem: FeedPost): Boolean {
            // Include userProfilePicture and username in equality check to trigger re-bind on profile updates
            return oldItem == newItem && 
                   oldItem.userProfilePicture == newItem.userProfilePicture &&
                   oldItem.username == newItem.username
        }
    }
}
