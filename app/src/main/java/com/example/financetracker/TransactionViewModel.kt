package com.example.financetracker

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(application)

    // — Selected month (drives all filtered LiveData) —
    val selectedMonth = MutableLiveData(
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    )

    val monthlyBudget: Double get() = repository.monthlyBudget

    // All transactions (unfiltered) — used only if needed globally
    val allTransactions: LiveData<List<Transaction>> = repository.getAll()

    // Transactions for the selected month
    val filteredTransactions: LiveData<List<Transaction>> = selectedMonth.switchMap { month ->
        repository.getByMonth(month)
    }

    // Derived summaries from filtered transactions
    val monthlyIncome: LiveData<Double> = filteredTransactions.map { list ->
        list.filter { it.type == TYPE_INCOME }.sumOf { it.amount }
    }

    val monthlyExpense: LiveData<Double> = filteredTransactions.map { list ->
        list.filter { it.type == TYPE_EXPENSE }.sumOf { it.amount }
    }

    val monthlyBalance: LiveData<Double> = filteredTransactions.map { list ->
        val inc = list.filter { it.type == TYPE_INCOME }.sumOf { it.amount }
        val exp = list.filter { it.type == TYPE_EXPENSE }.sumOf { it.amount }
        inc - exp
    }

    val budgetProgress: LiveData<BudgetState> = monthlyExpense.map { spent ->
        val budget = repository.monthlyBudget
        val percent = if (budget <= 0) 0 else ((spent / budget) * 100).toInt().coerceAtMost(100)
        BudgetState(spent, budget, percent)
    }

    // — Actions —

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun deleteById(id: Long) = viewModelScope.launch {
        repository.deleteById(id)
    }

    fun setMonthlyBudget(value: Double) {
        repository.monthlyBudget = value
    }

    fun selectMonth(yearMonth: String) {
        selectedMonth.value = yearMonth
    }

    data class BudgetState(val spent: Double, val budget: Double, val percent: Int)

    companion object {
        const val TYPE_INCOME = "Income"
        const val TYPE_EXPENSE = "Expense"
    }
}