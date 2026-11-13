package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PeerEntity
import com.example.data.model.UserAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM account LIMIT 1")
    fun getAccountFlow(): Flow<UserAccountEntity?>

    @Query("SELECT * FROM account LIMIT 1")
    suspend fun getAccount(): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAccount(account: UserAccountEntity)

    @Query("UPDATE account SET photoUri = :photoUri")
    suspend fun updatePhotoUri(photoUri: String?)

    @Query("UPDATE account SET bio = :bio")
    suspend fun updateBio(bio: String)

    @Query("DELETE FROM account")
    suspend fun clearAccount()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convoId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convoId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: String)

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :id")
    suspend fun updateMessageReaction(id: String, reaction: String?)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :convoId")
    suspend fun clearMessagesForConversation(convoId: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    @Query("SELECT COUNT(*) FROM messages WHERE deliveryRoute = 'SERVERLESS_RELAY'")
    fun getRelayMessagesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE deliveryRoute = 'P2P_DIRECT'")
    fun getP2PMessagesCount(): Flow<Int>
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE peerId = :peerId")
    suspend fun getConversation(peerId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(convo: ConversationEntity)

    @Query("UPDATE conversations SET isPeerOnline = :isOnline WHERE peerId = :peerId")
    suspend fun updatePeerOnlineStatus(peerId: String, isOnline: Boolean)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE peerId = :peerId")
    suspend fun clearUnread(peerId: String)

    @Query("DELETE FROM conversations WHERE peerId = :peerId")
    suspend fun deleteConversation(peerId: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()
}

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers ORDER BY isOnline DESC, displayName ASC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE peerId = :peerId")
    suspend fun getPeer(peerId: String): PeerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeer(peer: PeerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeers(peers: List<PeerEntity>)

    @Query("UPDATE peers SET isOnline = :isOnline, lastSeen = :timestamp WHERE peerId = :peerId")
    suspend fun updateOnlineStatus(peerId: String, isOnline: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM peers WHERE peerId = :peerId")
    suspend fun deletePeer(peerId: String)

    @Query("DELETE FROM peers")
    suspend fun clearAllPeers()
}
