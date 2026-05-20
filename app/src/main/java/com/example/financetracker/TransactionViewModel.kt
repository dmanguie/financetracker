// app/src/main/java/com/example/financetracker/TransactionViewModel.kt
package com.example.financetracker

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(application)
    private val auth = FirebaseAuth.getInstance()

    // ── Auth gate ──────────────────────────────────────────────────────────
    private val isAuthReady = MutableLiveData(auth.currentUser != null)

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        isAuthReady.postValue(firebaseAuth.currentUser != null)
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    // ── Selected month ─────────────────────────────────────────────────────
    val selectedMonth = MutableLiveData(
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    )

    val monthlyBudget: Double get() = repository.monthlyBudget

    // ── All transactions — waits for auth before querying ──────────────────
    val allTransactions: LiveData<List<Transaction>> =
        isAuthReady.switchMap { ready ->
            if (ready) repository.getAll()
            else MutableLiveData(emptyList())
        }

    // ── Filtered by month — waits for both auth AND selected month ─────────
    val filteredTransactions: LiveData<List<Transaction>> =
        isAuthReady.switchMap { ready ->
            if (!ready) return@switchMap MutableLiveData(emptyList())
            selectedMonth.switchMap { month ->
                repository.getByMonth(month)
            }
        }

    // ── Monthly summaries ──────────────────────────────────────────────────
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
        val percent = if (budget <= 0) 0
        else ((spent / budget) * 100).toInt().coerceAtMost(100)
        BudgetState(spent, budget, percent)
    }

    // ── Actions ────────────────────────────────────────────────────────────
    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun deleteById(id: String) = viewModelScope.launch {
        repository.deleteById(id)
    }

    fun setMonthlyBudget(value: Double) {
        repository.monthlyBudget = value
    }

    fun selectMonth(yearMonth: String) {
        selectedMonth.value = yearMonth
    }

    // ── Types ──────────────────────────────────────────────────────────────
    data class BudgetState(val spent: Double, val budget: Double, val percent: Int)

    companion object {
        const val TYPE_INCOME  = "Income"
        const val TYPE_EXPENSE = "Expense"
    }
}