package com.example.data.firebase

import android.util.Log
import com.example.data.db.MonthlyFinanceEntity
import com.example.data.db.SaleRecordEntity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages Firebase Firestore operations for storing and syncing sale records.
 */
class SaleFirestoreManager(
    private val firestoreInstance: FirebaseFirestore? = null
) {
    companion object {
        private const val TAG = "SaleFirestoreManager"
        const val COLLECTION_SALES = "sales_records"
        const val COLLECTION_MONTHLY_FINANCES = "monthly_finances"

        @Volatile
        private var INSTANCE: SaleFirestoreManager? = null

        fun getInstance(): SaleFirestoreManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SaleFirestoreManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Lazily initialized FirebaseFirestore instance with local cache persistence.
     */
    val firestore: FirebaseFirestore by lazy {
        firestoreInstance ?: try {
            FirebaseFirestore.getInstance().apply {
                try {
                    val cacheSettings = com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(cacheSettings)
                        .build()
                    firestoreSettings = settings
                } catch (e: Exception) {
                    Log.w(TAG, "Note on firestore settings: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FirebaseFirestore instance: ${e.message}", e)
            throw e
        }
    }

    /**
     * Reference to the 'sales_records' collection in Firestore.
     */
    val salesCollection: CollectionReference
        get() = firestore.collection(COLLECTION_SALES)

    /**
     * Converts a SaleRecordEntity into a Firestore-compatible Map.
     */
    fun toFirestoreDocument(sale: SaleRecordEntity): Map<String, Any> {
        return hashMapOf(
            "id" to sale.id,
            "invoiceNumber" to sale.invoiceNumber,
            "customerName" to sale.customerName,
            "customerPhone" to sale.customerPhone,
            "itemsSummary" to sale.itemsSummary,
            "itemsCount" to sale.itemsCount,
            "subtotal" to sale.subtotal,
            "discount" to sale.discount,
            "totalAmount" to sale.totalAmount,
            "paidAmount" to sale.paidAmount,
            "dueAmount" to sale.dueAmount,
            "paymentMethod" to sale.paymentMethod,
            "note" to sale.note,
            "timestamp" to sale.timestamp,
            "createdAtFormatted" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(sale.timestamp)),
            "syncedAt" to System.currentTimeMillis()
        )
    }

    /**
     * Stores a sale record in Firebase Firestore under document ID = invoiceNumber.
     * Uses SetOptions.merge() so existing records are updated rather than overwritten.
     */
    fun saveSale(
        sale: SaleRecordEntity,
        onComplete: ((success: Boolean, errorMsg: String?) -> Unit)? = null
    ) {
        try {
            val docData = toFirestoreDocument(sale)
            val docId = if (sale.invoiceNumber.isNotBlank()) sale.invoiceNumber else "SALE_${sale.timestamp}"

            salesCollection.document(docId)
                .set(docData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Sale record ${sale.invoiceNumber} successfully stored in Firestore")
                    onComplete?.invoke(true, null)
                }
                .addOnFailureListener { exception ->
                    val error = exception.localizedMessage ?: "Unknown Firestore error"
                    Log.e(TAG, "Error saving sale ${sale.invoiceNumber} to Firestore: $error", exception)
                    onComplete?.invoke(false, error)
                }
        } catch (e: Exception) {
            val error = e.localizedMessage ?: "Exception saving to Firestore"
            Log.e(TAG, error, e)
            onComplete?.invoke(false, error)
        }
    }

    /**
     * Asynchronously stores a sale record using coroutines.
     */
    suspend fun saveSaleAsync(sale: SaleRecordEntity): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            saveSale(sale) { success, errorMsg ->
                if (continuation.isActive) {
                    if (success) {
                        continuation.resume(Result.success(sale.invoiceNumber))
                    } else {
                        continuation.resume(Result.failure(Exception(errorMsg ?: "Firestore save failed")))
                    }
                }
            }
        }
    }

    /**
     * Deletes a sale record from Firestore by invoice number.
     */
    fun deleteSale(
        invoiceNumber: String,
        onComplete: ((success: Boolean) -> Unit)? = null
    ) {
        try {
            salesCollection.document(invoiceNumber)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Sale record $invoiceNumber deleted from Firestore")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to delete sale $invoiceNumber from Firestore: ${exception.message}", exception)
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting sale $invoiceNumber: ${e.message}", e)
            onComplete?.invoke(false)
        }
    }

    /**
     * Observes real-time sales records from Firestore as a Flow.
     */
    fun observeFirestoreSales(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listenerRegistration = try {
            salesCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val salesList = snapshot.documents.mapNotNull { it.data }
                    trySend(salesList)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching snapshot listener: ${e.message}", e)
            null
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    val financesCollection: CollectionReference
        get() = firestore.collection(COLLECTION_MONTHLY_FINANCES)

    fun toFirestoreFinanceDocument(finance: MonthlyFinanceEntity): Map<String, Any> {
        return hashMapOf(
            "id" to finance.id,
            "type" to finance.type,
            "title" to finance.title,
            "amount" to finance.amount,
            "category" to finance.category,
            "monthYear" to finance.monthYear,
            "note" to finance.note,
            "timestamp" to finance.timestamp,
            "createdAtFormatted" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(finance.timestamp)),
            "syncedAt" to System.currentTimeMillis()
        )
    }

    fun saveMonthlyFinance(
        finance: MonthlyFinanceEntity,
        onComplete: ((success: Boolean, errorMsg: String?) -> Unit)? = null
    ) {
        try {
            val docData = toFirestoreFinanceDocument(finance)
            val docId = if (finance.id != 0L) "FIN_${finance.type}_${finance.id}" else "FIN_${finance.type}_${finance.timestamp}"
            financesCollection.document(docId)
                .set(docData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully saved monthly finance $docId to Firestore")
                    onComplete?.invoke(true, null)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving monthly finance $docId to Firestore: ${e.message}", e)
                    onComplete?.invoke(false, e.message)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saving monthly finance: ${e.message}", e)
            onComplete?.invoke(false, e.message)
        }
    }

    fun deleteMonthlyFinance(
        id: Long,
        type: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        try {
            val docId = "FIN_${type}_$id"
            financesCollection.document(docId).delete()
                .addOnSuccessListener { onComplete?.invoke(true) }
                .addOnFailureListener { onComplete?.invoke(false) }
        } catch (e: Exception) {
            onComplete?.invoke(false)
        }
    }

    fun observeFirestoreFinances(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listenerRegistration = try {
            financesCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val financesList = snapshot.documents.mapNotNull { it.data }
                    trySend(financesList)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching snapshot listener: ${e.message}", e)
            null
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    /**
     * Asynchronously saves a monthly finance record using coroutines.
     */
    suspend fun saveMonthlyFinanceAsync(finance: MonthlyFinanceEntity): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            saveMonthlyFinance(finance) { success, errorMsg ->
                if (continuation.isActive) {
                    if (success) {
                        val docId = if (finance.id != 0L) "FIN_${finance.type}_${finance.id}" else "FIN_${finance.type}_${finance.timestamp}"
                        continuation.resume(Result.success(docId))
                    } else {
                        continuation.resume(Result.failure(Exception(errorMsg ?: "Failed to save monthly finance")))
                    }
                }
            }
        }
    }

    /**
     * Parses Firestore document Map to SaleRecordEntity.
     */
    fun fromFirestoreSaleDocument(data: Map<String, Any?>): SaleRecordEntity? {
        return try {
            val invoiceNumber = data["invoiceNumber"] as? String ?: return null
            val id = (data["id"] as? Number)?.toLong() ?: 0L
            val customerName = data["customerName"] as? String ?: ""
            val customerPhone = data["customerPhone"] as? String ?: ""
            val itemsSummary = data["itemsSummary"] as? String ?: "Sale item"
            val itemsCount = (data["itemsCount"] as? Number)?.toInt() ?: 1
            val subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0
            val discount = (data["discount"] as? Number)?.toDouble() ?: 0.0
            val totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0
            val paidAmount = (data["paidAmount"] as? Number)?.toDouble() ?: 0.0
            val dueAmount = (data["dueAmount"] as? Number)?.toDouble() ?: 0.0
            val paymentMethod = data["paymentMethod"] as? String ?: "CASH"
            val note = data["note"] as? String ?: ""
            val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()

            SaleRecordEntity(
                id = id,
                invoiceNumber = invoiceNumber,
                customerName = customerName,
                customerPhone = customerPhone,
                itemsSummary = itemsSummary,
                itemsCount = itemsCount,
                subtotal = subtotal,
                discount = discount,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                dueAmount = dueAmount,
                paymentMethod = paymentMethod,
                note = note,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Firestore sale document: ${e.message}", e)
            null
        }
    }

    /**
     * Parses Firestore document Map to MonthlyFinanceEntity.
     */
    fun fromFirestoreFinanceDocument(data: Map<String, Any?>): MonthlyFinanceEntity? {
        return try {
            val type = data["type"] as? String ?: return null
            val title = data["title"] as? String ?: return null
            val id = (data["id"] as? Number)?.toLong() ?: 0L
            val amount = (data["amount"] as? Number)?.toDouble() ?: 0.0
            val category = data["category"] as? String ?: ""
            val monthYear = data["monthYear"] as? String ?: ""
            val note = data["note"] as? String ?: ""
            val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()

            MonthlyFinanceEntity(
                id = id,
                type = type,
                title = title,
                amount = amount,
                category = category,
                monthYear = monthYear,
                note = note,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Firestore finance document: ${e.message}", e)
            null
        }
    }

    /**
     * Fetches all sale records from Firestore.
     */
    suspend fun fetchAllSalesFromFirestore(): List<SaleRecordEntity> {
        return suspendCancellableCoroutine { continuation ->
            try {
                salesCollection.get()
                    .addOnSuccessListener { snapshot ->
                        val list = snapshot.documents.mapNotNull { doc ->
                            fromFirestoreSaleDocument(doc.data ?: emptyMap())
                        }.sortedByDescending { it.timestamp }
                        if (continuation.isActive) continuation.resume(list)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to fetch sales from Firestore: ${exception.message}")
                        if (continuation.isActive) continuation.resume(emptyList())
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching sales: ${e.message}", e)
                if (continuation.isActive) continuation.resume(emptyList())
            }
        }
    }

    /**
     * Fetches all monthly finances from Firestore.
     */
    suspend fun fetchAllFinancesFromFirestore(): List<MonthlyFinanceEntity> {
        return suspendCancellableCoroutine { continuation ->
            try {
                financesCollection.get()
                    .addOnSuccessListener { snapshot ->
                        val list = snapshot.documents.mapNotNull { doc ->
                            fromFirestoreFinanceDocument(doc.data ?: emptyMap())
                        }.sortedByDescending { it.timestamp }
                        if (continuation.isActive) continuation.resume(list)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to fetch finances from Firestore: ${exception.message}")
                        if (continuation.isActive) continuation.resume(emptyList())
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching finances: ${e.message}", e)
                if (continuation.isActive) continuation.resume(emptyList())
            }
        }
    }

    /**
     * Syncs all sales and monthly finances to Firestore safely with async awaits.
     * Returns Pair(salesSyncedCount, financesSyncedCount).
     */
    suspend fun syncAllToFirestore(
        sales: List<SaleRecordEntity>,
        finances: List<MonthlyFinanceEntity>
    ): Pair<Int, Int> {
        var salesSynced = 0
        var financesSynced = 0

        for (sale in sales) {
            val result = saveSaleAsync(sale)
            if (result.isSuccess) {
                salesSynced++
            }
        }

        for (finance in finances) {
            val result = saveMonthlyFinanceAsync(finance)
            if (result.isSuccess) {
                financesSynced++
            }
        }

        return Pair(salesSynced, financesSynced)
    }
}

