package com.example.financetracker

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import java.text.SimpleDateFormat
import java.util.*

class TransactionRepository(context: Context) {

    private val dao: TransactionDao = TransactionDatabase.getInstance(context).transactionDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)

    var monthlyBudget: Double
        get() = prefs.getFloat("monthly_budget", 5000f).toDouble()
        set(value) = prefs.edit().putFloat("monthly_budget", value.toFloat()).apply()

    fun getAll(): LiveData<List<Transaction>> = dao.getAll()

    fun getByMonth(yearMonth: String): LiveData<List<Transaction>> {
        val (start, end) = yearMonthToRange(yearMonth)
        return dao.getByMonth(start, end)
    }

    suspend fun insert(transaction: Transaction) = dao.insert(transaction)

    suspend fun update(transaction: Transaction) = dao.update(transaction)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    companion object {
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