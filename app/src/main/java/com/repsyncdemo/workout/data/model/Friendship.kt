package com.repsyncdemo.workout.data.model

/**
 * File overview: Defines the Friendship data model used by repositories, view models, and UI binding.
 */

import com.google.firebase.firestore.DocumentId

data class Friendship(
    @DocumentId
    val id: String = "",
    val requesterId: String = "",
    val requesterUsername: String = "",
    val receiverId: String = "",
    val receiverUsername: String = "",
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class FriendshipStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
