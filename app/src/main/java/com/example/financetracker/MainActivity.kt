package com.example.financetracker

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TransactionAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvBudgetLabel: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var chipGroupMonths: ChipGroup
    private lateinit var tvEmptyState: TextView

    private var selectedMonth: String = ""

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

        // Default to current month
        selectedMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        setupMonthChips()

        adapter = TransactionAdapter(
            emptyList(),
            onEdit = { transaction ->
                startActivity(Intent(this, AddEditTransactionActivity::class.java).apply {
                    putExtra("EXTRA_TRANSACTION", transaction)
                })
            },
            onDelete = { id ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Transaction")
                    .setMessage("Remove this entry?")
                    .setPositiveButton("Delete") { _, _ ->
                        TransactionRepository.delete(id)
                        refreshAll()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddEditTransactionActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnSetBudget).setOnClickListener {
            showSetBudgetDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun setupMonthChips() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displaySdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()

        chipGroupMonths.removeAllViews()

        for (i in 5 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.MONTH, -i)
            val monthKey = sdf.format(cal.time)
            val label = displaySdf.format(cal.time)

            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = (monthKey == selectedMonth)
                setChipBackgroundColorResource(R.color.chip_selector)
                setTextColor(getColorStateList(R.color.chip_text_selector))
                tag = monthKey
            }
            chip.setOnClickListener {
                selectedMonth = monthKey
                for (j in 0 until chipGroupMonths.childCount) {
                    (chipGroupMonths.getChildAt(j) as Chip).isChecked =
                        chipGroupMonths.getChildAt(j).tag == monthKey
                }
                refreshAll()
            }
            chipGroupMonths.addView(chip)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshAll() {
        val all = TransactionRepository.getAll()
        val filtered = if (selectedMonth.isEmpty()) all
        else all.filter { it.date.startsWith(selectedMonth) }

        adapter.updateData(filtered)

        val balance = TransactionRepository.getBalance()
        tvBalance.text = "₱${String.format("%,.2f", balance)}"
        tvBalance.setTextColor(
            if (balance >= 0) getColor(R.color.income_green) else getColor(R.color.expense_red)
        )

        tvIncome.text = "₱${String.format("%,.2f", TransactionRepository.getTotalIncome())}"
        tvExpense.text = "₱${String.format("%,.2f", TransactionRepository.getTotalExpense())}"

        // Budget progress for selected month
        val spent = TransactionRepository.getExpenseByMonth(selectedMonth)
        val budget = TransactionRepository.monthlyBudget
        val percent = ((spent / budget) * 100).toInt().coerceAtMost(100)
        progressBudget.progress = percent
        tvBudgetLabel.text = "Budget: ₱${String.format("%,.0f", spent)} / ₱${String.format("%,.0f", budget)} ($percent%)"

        tvEmptyState.visibility = if (filtered.isEmpty()) TextView.VISIBLE else TextView.GONE
        recyclerView.visibility = if (filtered.isEmpty()) RecyclerView.GONE else RecyclerView.VISIBLE
    }

    private fun showSetBudgetDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter monthly budget"
            setText(TransactionRepository.monthlyBudget.toInt().toString())
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Set Monthly Budget")
            .setView(input)
            .setPositiveButton("Set") { _, _ ->
                val value = input.text.toString().toDoubleOrNull()
                if (value != null && value > 0) {
                    TransactionRepository.monthlyBudget = value
                    refreshAll()
                } else {
                    Toast.makeText(this, "Invalid budget amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}