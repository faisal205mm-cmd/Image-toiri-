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

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales_records ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleRecordEntity): Long

    @Delete
    suspend fun deleteSale(sale: SaleRecordEntity)

    @Query("DELETE FROM sales_records WHERE id = :id")
    suspend fun deleteSaleById(id: Long)

    @Query("SELECT COUNT(*) FROM sales_records")
    fun getSalesCount(): Flow<Int>

    @Query("SELECT SUM(totalAmount) FROM sales_records")
    fun getTotalRevenue(): Flow<Double?>
}

@Dao
interface MonthlyFinanceDao {
    @Query("SELECT * FROM monthly_finances ORDER BY timestamp DESC")
    fun getAllFinances(): Flow<List<MonthlyFinanceEntity>>

    @Query("SELECT * FROM monthly_finances WHERE type = :type ORDER BY timestamp DESC")
    fun getFinancesByType(type: String): Flow<List<MonthlyFinanceEntity>>

    @Query("SELECT SUM(amount) FROM monthly_finances WHERE type = :type")
    fun getTotalByType(type: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinance(finance: MonthlyFinanceEntity): Long

    @Delete
    suspend fun deleteFinance(finance: MonthlyFinanceEntity)

    @Query("DELETE FROM monthly_finances WHERE id = :id")
    suspend fun deleteFinanceById(id: Long)

    @Query("DELETE FROM monthly_finances")
    suspend fun clearAllFinances()
}

