// app/src/main/java/com/example/financetracker/Transaction.kt
package com.example.financetracker

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

/**
 * Data model for a single financial transaction.
 *
 * @DocumentId tells Firestore to automatically map the document's ID
 * into the [id] field when reading, and to NOT store [id] as a
 * separate field inside the document (Firestore manages it).
 *
 * All fields need default values so Firestore can create instances
 * via reflection when reading documents.
 */
data class Transaction(
    @DocumentId val id: String = "",   // Firestore document ID (auto-generated)
    var title: String = "",
    var amount: Double = 0.0,
    var type: String = "",             // "Income" or "Expense"
    var dateMillis: Long = 0L,         // UTC timestamp
    var category: String = ""
) : Serializable