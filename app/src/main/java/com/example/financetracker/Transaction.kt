// app/src/main/java/com/example/financetracker/Transaction.kt
package com.example.financetracker

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Transaction(
    @DocumentId val id: String = "",   // Firestore document ID (auto-generated)
    var title: String = "",
    var amount: Double = 0.0,
    var type: String = "",             // "Income" or "Expense"
    var dateMillis: Long = 0L,         // UTC timestamp
    var category: String = ""
) : Serializable