// app/src/main/java/com/example/financetracker/TransactionRepository.kt
package com.example.financetracker

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Single source of truth for transaction data.
 *
 * Reads  → Firestore real-time listeners exposed as LiveData
 * Writes → Kotlin suspend functions (run in a coroutine scope)
 * Budget → SharedPreferences (local, no sync needed)
 */
class TransactionRepository(context: Context) {

    // ── Firestore ──────────────────────────────────────────────────────────
    private val db = FirebaseFirestore.getInstance()

    /**
     * "transactions" is the Firestore collection name.
     * Every document in it is one Transaction.
     */
    private val collection = db.collection("transactions")

    // ── Budget (local only) ────────────────────────────────────────────────
    private val prefs: SharedPreferences =
        context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)

    var monthlyBudget: Double
        get() = prefs.getFloat("monthly_budget", 5000f).toDouble()
        set(value) = prefs.edit().putFloat("monthly_budget", value.toFloat()).apply()

    // ── Read operations ────────────────────────────────────────────────────

    /**
     * Returns ALL transactions ordered by date (newest first).
     * Used by the Dashboard.
     */
    fun getAll(): LiveData<List<Transaction>> =
        FirestoreTransactionLiveData(
            collection.orderBy("dateMillis", Query.Direction.DESCENDING)
        )

    /**
     * Returns transactions for a specific month, e.g. "2025-05".
     * Used by the Transactions screen (MainActivity) with chip filter.
     */
    fun getByMonth(yearMonth: String): LiveData<List<Transaction>> {
        val (start, end) = yearMonthToRange(yearMonth)
        return FirestoreTransactionLiveData(
            collection
                .whereGreaterThanOrEqualTo("dateMillis", start)
                .whereLessThan("dateMillis", end)
                .orderBy("dateMillis", Query.Direction.DESCENDING)
        )
    }

    // ── Write operations (suspend = call from a coroutine) ─────────────────

    /**
     * Adds a new transaction. Firestore auto-generates the document ID.
     * The [transaction.id] field is ignored on insert (it will be
     * filled in when you read it back, via @DocumentId).
     */
    suspend fun insert(transaction: Transaction) {
        collection.add(transaction).await()
    }

    /**
     * Overwrites the existing document that matches [transaction.id].
     */
    suspend fun update(transaction: Transaction) {
        collection.document(transaction.id).set(transaction).await()
    }

    /**
     * Deletes the document with the given Firestore document ID.
     */
    suspend fun deleteById(id: String) {
        collection.document(id).delete().await()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    companion object {
        /**
         * Converts "2025-05" → (startMillis, endMillis) for a Firestore
         * range query covering that entire calendar month.
         */
        fun yearMonthToRange(yearMonth: String): Pair<Long, Long> {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(yearMonth) ?: Date()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            return start to cal.timeInMillis
        }
    }
}