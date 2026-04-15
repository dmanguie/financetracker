package com.example.financetracker

import java.io.Serializable

data class Transaction(
    val id: Long,
    var title: String,
    var amount: Double,
    var type: String,       // "Income" or "Expense"
    var date: String,       // "YYYY-MM-DD"
    var category: String    // e.g. "Food", "Transport", "School", etc.
) : Serializable