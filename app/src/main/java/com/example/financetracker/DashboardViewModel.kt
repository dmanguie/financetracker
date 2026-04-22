package com.example.financetracker

import android.app.Application
import androidx.lifecycle.*
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(application)

    val allTransactions: LiveData<List<Transaction>> = repository.getAll()

    val totalBalance: LiveData<Double> = allTransactions.map { list ->
        val income = list.filter { it.type == TransactionViewModel.TYPE_INCOME }.sumOf { it.amount }
        val expense = list.filter { it.type == TransactionViewModel.TYPE_EXPENSE }.sumOf { it.amount }
        income - expense
    }

    val totalIncome: LiveData<Double> = allTransactions.map { list ->
        list.filter { it.type == TransactionViewModel.TYPE_INCOME }.sumOf { it.amount }
    }

    val totalExpense: LiveData<Double> = allTransactions.map { list ->
        list.filter { it.type == TransactionViewModel.TYPE_EXPENSE }.sumOf { it.amount }
    }

    val totalTransactionCount: LiveData<Int> = allTransactions.map { it.size }

    val recentTransactions: LiveData<List<Transaction>> = allTransactions.map { list ->
        list.take(5)
    }

    val currentMonthExpense: LiveData<Double> = allTransactions.map { list ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val (start, end) = TransactionRepository.yearMonthToRange(currentMonth)
        list.filter {
            it.type == TransactionViewModel.TYPE_EXPENSE &&
                    it.dateMillis in start until end
        }.sumOf { it.amount }
    }
}