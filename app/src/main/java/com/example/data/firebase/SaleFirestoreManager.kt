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
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
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
}

