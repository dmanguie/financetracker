package com.example.financetracker

object TransactionRepository {
    private val transactions = mutableListOf<Transaction>()
    var monthlyBudget: Double = 5000.0

    fun getAll(): List<Transaction> = transactions.sortedByDescending { it.date }

    fun add(transaction: Transaction) = transactions.add(transaction)

    fun update(updated: Transaction) {
        val index = transactions.indexOfFirst { it.id == updated.id }
        if (index != -1) transactions[index] = updated
    }

    fun delete(id: Long) = transactions.removeAll { it.id == id }

    fun getTotalIncome(): Double =
        transactions.filter { it.type == "Income" }.sumOf { it.amount }

    fun getTotalExpense(): Double =
        transactions.filter { it.type == "Expense" }.sumOf { it.amount }

    fun getBalance(): Double = getTotalIncome() - getTotalExpense()

    fun getExpenseByMonth(yearMonth: String): Double =
        transactions.filter { it.type == "Expense" && it.date.startsWith(yearMonth) }.sumOf { it.amount }
}