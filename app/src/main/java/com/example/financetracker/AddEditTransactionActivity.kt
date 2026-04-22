package com.example.financetracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.text.SimpleDateFormat
import java.util.*

class AddEditTransactionActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TransactionViewModel(application) as T
            }
        }
    }

    private var editingTransaction: Transaction? = null
    private var selectedDateMillis: Long = System.currentTimeMillis()

    private val categories = listOf(
        "Food", "Transport", "School", "Entertainment",
        "Health", "Shopping", "Allowance", "Part-time", "Savings", "Other"
    )

    private val displaySdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_transaction)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val tvDate = findViewById<TextView>(R.id.tvSelectedDate)
        val btnPickDate = findViewById<Button>(R.id.btnPickDate)
        val rgType = findViewById<RadioGroup>(R.id.rgType)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<ImageButton>(R.id.btnCancel)

        // Spinner setup
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        // Default date = today
        selectedDateMillis = System.currentTimeMillis()
        tvDate.text = displaySdf.format(Date(selectedDateMillis))

        // Date picker
        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            DatePickerDialog(this, { _, y, m, d ->
                val picked = Calendar.getInstance().apply {
                    set(y, m, d, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedDateMillis = picked.timeInMillis
                tvDate.text = displaySdf.format(Date(selectedDateMillis))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        @Suppress("DEPRECATION")
        editingTransaction = intent.getSerializableExtra(EXTRA_TRANSACTION) as? Transaction

        // ─────────────────────────────────────────────
        // ✅ HANDLE PRESET TYPE (from Dashboard buttons)
        // ─────────────────────────────────────────────
        val presetType = intent.getStringExtra("PRESET_TYPE")

        // ─────────────────────────────────────────────
        // EDIT MODE
        // ─────────────────────────────────────────────
        editingTransaction?.let { t ->
            supportActionBar?.title = getString(R.string.edit_transaction)

            etTitle.setText(t.title)
            etAmount.setText(t.amount.toString())
            selectedDateMillis = t.dateMillis
            tvDate.text = displaySdf.format(Date(t.dateMillis))

            if (t.type == TransactionViewModel.TYPE_INCOME) {
                findViewById<RadioButton>(R.id.rbIncome).isChecked = true
            } else {
                findViewById<RadioButton>(R.id.rbExpense).isChecked = true
            }

            spinnerCategory.setSelection(categories.indexOf(t.category).coerceAtLeast(0))
        } ?: run {
            // ─────────────────────────────────────────────
            // ADD MODE
            // ─────────────────────────────────────────────
            supportActionBar?.title = getString(R.string.add_transaction)

            // Apply preset ONLY if not editing
            if (presetType == TransactionViewModel.TYPE_INCOME) {
                findViewById<RadioButton>(R.id.rbIncome).isChecked = true
            } else if (presetType == TransactionViewModel.TYPE_EXPENSE) {
                findViewById<RadioButton>(R.id.rbExpense).isChecked = true
            }
        }

        btnCancel.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val titleText = etTitle.text.toString().trim()
            val amountText = etAmount.text.toString().trim()
            val selectedTypeId = rgType.checkedRadioButtonId
            val category = spinnerCategory.selectedItem.toString()

            if (titleText.isEmpty()) {
                etTitle.error = getString(R.string.error_required)
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                etAmount.error = getString(R.string.error_invalid_amount)
                return@setOnClickListener
            }

            if (selectedTypeId == -1) {
                Toast.makeText(this, getString(R.string.error_select_type), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = if (selectedTypeId == R.id.rbIncome)
                TransactionViewModel.TYPE_INCOME
            else
                TransactionViewModel.TYPE_EXPENSE

            if (editingTransaction == null) {
                viewModel.insert(
                    Transaction(
                        title = titleText,
                        amount = amount,
                        type = type,
                        dateMillis = selectedDateMillis,
                        category = category
                    )
                )
            } else {
                viewModel.update(
                    editingTransaction!!.copy(
                        title = titleText,
                        amount = amount,
                        type = type,
                        dateMillis = selectedDateMillis,
                        category = category
                    )
                )
            }

            finish()
        }
    }

    companion object {
        const val EXTRA_TRANSACTION = "EXTRA_TRANSACTION"
    }
}