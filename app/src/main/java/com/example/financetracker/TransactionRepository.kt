// app/src/main/java/com/example/financetracker/TransactionRepository.kt
package com.example.financetracker

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Single source of truth for transaction data.
 *
 * Firestore path:  users/{uid}/transactions/{docId}
 *
 * Each signed-in user has their own sub-collection so transactions
 * are private — no user can see another's data.
 *
 * Reads  → real-time LiveData via [FirestoreTransactionLiveData]
 * Writes → Kotlin suspend functions (call inside a coroutine scope)
 * Budget → SharedPreferences (local device storage)
 */
class TransactionRepository(context: Context) {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Returns the Firestore sub-collection for the current user.
     * Using a computed property (get()) ensures we always use the
     * correct UID even if the user changes during the app session.
     *
     * Path: users / {uid} / transactions
     */
    private val collection
        get() = db
            .collection("users")
            .document(auth.currentUser?.uid ?: "anonymous")
            .collection("transactions")

    // ── Budget (stored locally) ────────────────────────────────────────────
    private val prefs: SharedPreferences =
        context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)

    var monthlyBudget: Double
        get() = prefs.getFloat("monthly_budget", 5000f).toDouble()
        set(value) = prefs.edit().putFloat("monthly_budget", value.toFloat()).apply()

    // ── Read operations ────────────────────────────────────────────────────

    /** All transactions for the current user, newest first. */
    fun getAll(): LiveData<List<Transaction>> =
        FirestoreTransactionLiveData(
            collection.orderBy("dateMillis", Query.Direction.DESCENDING)
        )

    /** Transactions for a specific month, e.g. "2025-05". */
    fun getByMonth(yearMonth: String): LiveData<List<Transaction>> {
        val (start, end) = yearMonthToRange(yearMonth)
        return FirestoreTransactionLiveData(
            collection
                .whereGreaterThanOrEqualTo("dateMillis", start)
                .whereLessThan("dateMillis", end)
                .orderBy("dateMillis", Query.Direction.DESCENDING)
        )
    }

    // ── Write operations ───────────────────────────────────────────────────

    /** Adds a new transaction. Firestore generates the document ID. */
    suspend fun insert(transaction: Transaction) {
        collection.add(transaction).await()
    }

    /** Replaces the document matching [transaction.id] with new data. */
    suspend fun update(transaction: Transaction) {
        collection.document(transaction.id).set(transaction).await()
    }

    /** Deletes the document with the given Firestore document ID. */
    suspend fun deleteById(id: String) {
        collection.document(id).delete().await()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    companion object {
        /**
         * Converts "2025-05" → (startMillis, endMillis) for a Firestore
         * range query covering the entire calendar month.
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