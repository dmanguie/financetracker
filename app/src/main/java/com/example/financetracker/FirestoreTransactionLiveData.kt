// app/src/main/java/com/example/financetracker/FirestoreTransactionLiveData.kt
package com.example.financetracker

import androidx.lifecycle.LiveData
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * A LiveData that wraps a Firestore [Query] and pushes updates
 * every time the matching documents change in the cloud.
 *
 * - [onActive]   → starts the Firestore snapshot listener
 * - [onInactive] → removes it (no observers = no network calls)
 */
class FirestoreTransactionLiveData(
    private val query: Query
) : LiveData<List<Transaction>>() {

    private var registration: ListenerRegistration? = null

    override fun onActive() {
        super.onActive()
        registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                // Post an empty list on error so the UI doesn't hang
                postValue(emptyList())
                return@addSnapshotListener
            }
            // Convert each Firestore document into a Transaction object
            val transactions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)
            }
            postValue(transactions)
        }
    }

    override fun onInactive() {
        super.onInactive()
        registration?.remove()
        registration = null
    }
}