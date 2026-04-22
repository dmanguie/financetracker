package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private val viewModel: DashboardViewModel by lazy {
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return DashboardViewModel(application) as T
                }
            }
        )[DashboardViewModel::class.java]
    }

    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvMonthExpense: TextView
    private lateinit var tvTxCount: TextView
    private lateinit var tvRecentEmpty: TextView
    private lateinit var llRecentList: LinearLayout
    private lateinit var tvSeeAll: TextView
    private lateinit var btnGoToTransactions: com.google.android.material.button.MaterialButton
    private lateinit var btnQuickAddIncome: com.google.android.material.button.MaterialButton
    private lateinit var btnQuickAddExpense: com.google.android.material.button.MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        bindViews()
        observeViewModel()
        setupNavigation()
    }

    private fun bindViews() {
        tvBalance = findViewById(R.id.tvDashBalance)
        tvIncome = findViewById(R.id.tvDashIncome)
        tvExpense = findViewById(R.id.tvDashExpense)
        tvMonthExpense = findViewById(R.id.tvDashMonthExpense)
        tvTxCount = findViewById(R.id.tvDashTxCount)
        tvRecentEmpty = findViewById(R.id.tvRecentEmpty)
        llRecentList = findViewById(R.id.llRecentList)
        tvSeeAll = findViewById(R.id.tvSeeAll)
        btnGoToTransactions = findViewById(R.id.btnGoToTransactions)
        btnQuickAddIncome = findViewById(R.id.btnQuickAddIncome)
        btnQuickAddExpense = findViewById(R.id.btnQuickAddExpense)
    }

    private fun observeViewModel() {
        viewModel.totalBalance.observe(this) { balance ->
            tvBalance.text = formatCurrency(balance)
            tvBalance.setTextColor(
                if (balance >= 0) getColor(R.color.income_green)
                else getColor(R.color.expense_red)
            )
        }

        viewModel.totalIncome.observe(this) { income ->
            tvIncome.text = formatCurrency(income)
        }

        viewModel.totalExpense.observe(this) { expense ->
            tvExpense.text = formatCurrency(expense)
        }

        viewModel.currentMonthExpense.observe(this) { spent ->
            tvMonthExpense.text = formatCurrency(spent)
        }

        viewModel.totalTransactionCount.observe(this) { count ->
            tvTxCount.text = count.toString()
        }

        viewModel.recentTransactions.observe(this) { transactions ->
            populateRecentList(transactions)
        }
    }

    private fun populateRecentList(transactions: List<Transaction>) {
        llRecentList.removeAllViews()

        if (transactions.isEmpty()) {
            tvRecentEmpty.visibility = View.VISIBLE
            llRecentList.visibility = View.GONE
            return
        }

        tvRecentEmpty.visibility = View.GONE
        llRecentList.visibility = View.VISIBLE

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val inflater = LayoutInflater.from(this)

        transactions.forEachIndexed { index, tx ->
            val itemView = inflater.inflate(R.layout.item_recent_transaction_dash, llRecentList, false)

            itemView.findViewById<TextView>(R.id.tvRecentEmoji).text =
                TransactionAdapter.getCategoryEmoji(tx.category)

            itemView.findViewById<TextView>(R.id.tvRecentTitle).text = tx.title

            itemView.findViewById<TextView>(R.id.tvRecentDate).text =
                sdf.format(Date(tx.dateMillis))

            val tvAmount = itemView.findViewById<TextView>(R.id.tvRecentAmount)
            if (tx.type == TransactionViewModel.TYPE_INCOME) {
                tvAmount.text = "+₱${String.format("%,.2f", tx.amount)}"
                tvAmount.setTextColor(getColor(R.color.income_green))
            } else {
                tvAmount.text = "-₱${String.format("%,.2f", tx.amount)}"
                tvAmount.setTextColor(getColor(R.color.expense_red))
            }

            llRecentList.addView(itemView)

            // Add a thin divider between items (not after last)
            if (index < transactions.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).also { it.setMargins(52, 0, 0, 0) }
                    setBackgroundColor(0x1AFFFFFF)
                }
                llRecentList.addView(divider)
            }
        }
    }

    private fun setupNavigation() {
        val goToMain = Intent(this, MainActivity::class.java)

        btnGoToTransactions.setOnClickListener {
            startActivity(goToMain)
        }

        tvSeeAll.setOnClickListener {
            startActivity(goToMain)
        }

        btnQuickAddIncome.setOnClickListener {
            startActivity(
                Intent(this, AddEditTransactionActivity::class.java).apply {
                    putExtra("PRESET_TYPE", TransactionViewModel.TYPE_INCOME)
                }
            )
        }

        btnQuickAddExpense.setOnClickListener {
            startActivity(
                Intent(this, AddEditTransactionActivity::class.java).apply {
                    putExtra("PRESET_TYPE", TransactionViewModel.TYPE_EXPENSE)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // LiveData auto-refreshes — no manual refresh needed
    }

    private fun formatCurrency(amount: Double): String =
        "₱${String.format("%,.2f", amount)}"
}