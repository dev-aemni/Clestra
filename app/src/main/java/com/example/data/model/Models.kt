package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * User Account Entity storing permanent identity
 */
@Entity(tableName = "account")
data class UserAccountEntity(
    @PrimaryKey
    val id: String, // Immutable Clestra ID (e.g. CLS-XXXX-XXXX-XXXX)
    val displayName: String,
    val username: String = "",
    val bio: String = "",
    val photoUri: String? = null,
    val avatarIcon: String = "🍑",
    val avatarColor: Long = 0xFFC4763A,
    val passwordHash: String,
    val activeTag: String? = null,
    val unlockedTags: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Message Entity stored locally in Room database
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val compressedContent: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryRoute: String, // "P2P_DIRECT" or "SERVERLESS_RELAY"
    val status: String,        // "PENDING", "SENT", "DELIVERED", "READ"
    val isOutgoing: Boolean,
    val reaction: String? = null,
    val mediaUri: String? = null,
    val messageType: String = "TEXT", // "TEXT", "IMAGE", "GIF", "STICKER", "FILE"
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentSizeBytes: Long = 0L,
    val replyToMessageId: String? = null,
    val replyToPreview: String? = null,
    val replyToSenderName: String? = null,
    val isStarred: Boolean = false,
    val isEdited: Boolean = false
)

/**
 * Custom & Built-in Stickers stored locally in Room
 */
@Entity(tableName = "custom_stickers")
data class CustomStickerEntity(
    @PrimaryKey
    val id: String,
    val label: String,
    val uri: String,
    val packName: String = "My Stickers",
    val isCustom: Boolean = true,
    val styleTag: String = "CLEAN",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Conversation entity for chat list preview
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val peerId: String,
    val peerName: String,
    val peerAvatarColor: Long, // Hex color for avatar
    val peerAvatarIcon: String = "🍑",
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0,
    val isPeerOnline: Boolean = true, // Whether peer is currently reachable via P2P
    val lastDeliveryRoute: String = "P2P_DIRECT",
    val isOfficial: Boolean = false,
    val isMuted: Boolean = false,
    val mutedUntil: Long? = null,
    val isPinned: Boolean = false,
    val isBlocked: Boolean = false
)

/**
 * Peer Entity representing contacts in the decentralized mesh
 */
@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey
    val peerId: String,
    val displayName: String,
    val bio: String = "",
    val avatarColor: Long = 0xFFFF7043,
    val avatarIcon: String = "🍑",
    val isOnline: Boolean = true,
    val directAddress: String = "p2p://local-node",
    val lastSeen: Long = System.currentTimeMillis(),
    val isOfficial: Boolean = false,
    val isMuted: Boolean = false,
    val mutedUntil: Long? = null,
    val isBlocked: Boolean = false
)

// --- Serverless Relay API DTOs ---

@JsonClass(generateAdapter = true)
data class SendMessagePayload(
    @Json(name = "senderId") val senderId: String,
    @Json(name = "recipientId") val recipientId: String,
    @Json(name = "compressedContent") val compressedContent: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "pulse") val pulse: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class SendMessageResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "messageId") val messageId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class PendingMessageItem(
    @Json(name = "id") val id: String?,
    @Json(name = "senderId") val senderId: String,
    @Json(name = "recipientId") val recipientId: String,
    @Json(name = "compressedContent") val compressedContent: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "delivered") val delivered: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "recipientId") val recipientId: String?,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "messages") val messages: List<PendingMessageItem> = emptyList(),
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class RelayHealthResponse(
    @Json(name = "service") val service: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "mongodb") val mongodb: String?,
    @Json(name = "timestamp") val timestamp: Long?
)
