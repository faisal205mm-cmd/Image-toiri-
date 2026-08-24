package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // --- Sessions ---
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionById(sessionId: Long): Flow<ChatSessionEntity?>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionByIdDirect(sessionId: Long): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET title = :newTitle WHERE id = :id")
    suspend fun renameSession(id: Long, newTitle: String)

    @Query("UPDATE chat_sessions SET title = :title, lastSnippet = :snippet, updatedAt = :updatedAt, messageCount = :count WHERE id = :id")
    suspend fun updateSessionMeta(id: Long, title: String, snippet: String, updatedAt: Long, count: Int)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllSessions()

    // --- Messages ---
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionDirect(sessionId: Long): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE persona = :persona ORDER BY timestamp ASC")
    fun getMessagesByPersona(persona: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM chat_messages WHERE persona = :persona")
    suspend fun clearMessagesForPersona(persona: String)
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY timestamp DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)
}

@Dao
interface PricingDao {
    @Query("SELECT * FROM pricing_skus ORDER BY timestamp DESC")
    fun getAllSkus(): Flow<List<PricingSkuEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSku(sku: PricingSkuEntity): Long

    @Delete
    suspend fun deleteSku(sku: PricingSkuEntity)
}

@Dao
interface BrandDao {
    @Query("SELECT * FROM brand_profile WHERE id = 1")
    fun getProfileFlow(): Flow<BrandProfileEntity?>

    @Query("SELECT * FROM brand_profile WHERE id = 1")
    suspend fun getProfile(): BrandProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: BrandProfileEntity)
}
