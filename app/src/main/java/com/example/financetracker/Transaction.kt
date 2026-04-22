package com.example.financetracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var title: String,
    var amount: Double,
    var type: String,       // "Income" or "Expense"
    var dateMillis: Long,   // stored as timestamp; display with DateUtils
    var category: String
) : Serializable