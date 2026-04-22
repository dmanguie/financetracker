package com.example.financetracker

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TransactionViewModel(application) as T
            }
        }
    }

    private lateinit var adapter: TransactionAdapter
    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvBudgetLabel: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var chipGroupMonths: ChipGroup
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBalance = findViewById(R.id.tvBalance)
        tvIncome = findViewById(R.id.tvIncome)
        tvExpense = findViewById(R.id.tvExpense)
        tvBudgetLabel = findViewById(R.id.tvBudgetLabel)
        progressBudget = findViewById(R.id.progressBudget)
        chipGroupMonths = findViewById(R.id.chipGroupMonths)
        recyclerView = findViewById(R.id.recyclerView)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        adapter = TransactionAdapter(
            onEdit = { transaction ->
                startActivity(Intent(this, AddEditTransactionActivity::class.java).apply {
                    putExtra(AddEditTransactionActivity.EXTRA_TRANSACTION, transaction)
                })
            },
            onDelete = { id ->
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_title))
                    .setMessage(getString(R.string.delete_message))
                    .setPositiveButton(getString(R.string.delete_confirm)) { _, _ ->
                        viewModel.deleteById(id)
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupMonthChips()
        observeViewModel()

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddEditTransactionActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnSetBudget).setOnClickListener {
            showSetBudgetDialog()
        }
    }

    private fun setupMonthChips() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displaySdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val currentMonth = viewModel.selectedMonth.value ?: ""

        chipGroupMonths.removeAllViews()
        val cal = Calendar.getInstance()

        for (i in 5 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.MONTH, -i)
            val monthKey = sdf.format(cal.time)
            val label = displaySdf.format(cal.time)

            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = (monthKey == currentMonth)
                setChipBackgroundColorResource(R.color.chip_selector)
                setTextColor(getColorStateList(R.color.chip_text_selector))
                tag = monthKey
            }
            chip.setOnClickListener {
                viewModel.selectMonth(monthKey)
                for (j in 0 until chipGroupMonths.childCount) {
                    (chipGroupMonths.getChildAt(j) as Chip).isChecked =
                        chipGroupMonths.getChildAt(j).tag == monthKey
                }
            }
            chipGroupMonths.addView(chip)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeViewModel() {
        viewModel.filteredTransactions.observe(this) { list ->
            adapter.submitList(list)
            tvEmptyState.visibility = if (list.isEmpty()) TextView.VISIBLE else TextView.GONE
            recyclerView.visibility = if (list.isEmpty()) RecyclerView.GONE else RecyclerView.VISIBLE
        }

        viewModel.monthlyBalance.observe(this) { balance ->
            tvBalance.text = getString(R.string.currency_format, balance)
            tvBalance.setTextColor(
                if (balance >= 0) getColor(R.color.income_green) else getColor(R.color.expense_red)
            )
        }

        viewModel.monthlyIncome.observe(this) { income ->
            tvIncome.text = getString(R.string.currency_format, income)
        }

        viewModel.monthlyExpense.observe(this) { expense ->
            tvExpense.text = getString(R.string.currency_format, expense)
        }

        viewModel.budgetProgress.observe(this) { state ->
            progressBudget.progress = state.percent
            tvBudgetLabel.text = getString(
                R.string.budget_label,
                state.spent, state.budget, state.percent
            )
        }
    }

    private fun showSetBudgetDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = getString(R.string.budget_hint)
            setText(viewModel.monthlyBudget.toInt().toString())
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.budget_dialog_title))
            .setView(input)
            .setPositiveButton(getString(R.string.budget_set)) { _, _ ->
                val value = input.text.toString().toDoubleOrNull()
                if (value != null && value > 0) {
                    viewModel.setMonthlyBudget(value)
                    viewModel.budgetProgress.value?.let { /* triggers re-observe */ }
                } else {
                    Toast.makeText(this, getString(R.string.error_invalid_budget), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}