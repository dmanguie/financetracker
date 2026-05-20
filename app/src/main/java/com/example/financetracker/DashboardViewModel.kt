// app/src/main/java/com/example/financetracker/DashboardViewModel.kt
package com.example.financetracker

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(application)
    private val auth = FirebaseAuth.getInstance()

    // ── Auth gate ──────────────────────────────────────────────────────────
    // Only becomes true once Firebase confirms a logged-in user.
    // Everything else derives from this so nothing queries Firestore
    // until auth is ready.
    private val isAuthReady = MutableLiveData<Boolean>(false)

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

    // ── Data — only loads after auth is confirmed ──────────────────────────
    val allTransactions: LiveData<List<Transaction>> =
        isAuthReady.switchMap { ready ->
            if (ready) repository.getAll()
            else MutableLiveData(emptyList())
        }

    val totalBalance: LiveData<Double> = allTransactions.map { list ->
        val income  = list.filter { it.type == TransactionViewModel.TYPE_INCOME  }.sumOf { it.amount }
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