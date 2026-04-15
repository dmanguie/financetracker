package com.example.financetracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AddEditTransactionActivity : AppCompatActivity() {

    private var editingTransaction: Transaction? = null
    private var selectedDate: String = ""

    private val categories = listOf(
        "Food", "Transport", "School", "Entertainment",
        "Health", "Shopping", "Allowance", "Part-time", "Savings", "Other"
    )

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
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        selectedDate = today
        tvDate.text = today

        // Date picker
        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate = "%04d-%02d-%02d".format(y, m + 1, d)
                tvDate.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        editingTransaction = intent.getSerializableExtra("EXTRA_TRANSACTION") as? Transaction

        editingTransaction?.let {
            supportActionBar?.title = "Edit Transaction"
            etTitle.setText(it.title)
            etAmount.setText(it.amount.toString())
            selectedDate = it.date
            tvDate.text = it.date
            if (it.type == "Income") {
                findViewById<RadioButton>(R.id.rbIncome).isChecked = true
            } else {
                findViewById<RadioButton>(R.id.rbExpense).isChecked = true
            }
            val catIndex = categories.indexOf(it.category).coerceAtLeast(0)
            spinnerCategory.setSelection(catIndex)
        } ?: run {
            supportActionBar?.title = "Add Transaction"
        }

        btnCancel.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val titleText = etTitle.text.toString().trim()
            val amountText = etAmount.text.toString().trim()
            val selectedTypeId = rgType.checkedRadioButtonId
            val category = spinnerCategory.selectedItem.toString()

            if (titleText.isEmpty()) { etTitle.error = "Required"; return@setOnClickListener }
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) { etAmount.error = "Enter valid amount"; return@setOnClickListener }
            if (selectedTypeId == -1) { Toast.makeText(this, "Select a type", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val type = if (selectedTypeId == R.id.rbIncome) "Income" else "Expense"

            if (editingTransaction == null) {
                TransactionRepository.add(
                    Transaction(System.currentTimeMillis(), titleText, amount, type, selectedDate, category)
                )
            } else {
                editingTransaction!!.apply {
                    title = titleText
                    this.amount = amount
                    this.type = type
                    date = selectedDate
                    this.category = category
                }
                TransactionRepository.update(editingTransaction!!)
            }
            finish()
        }
    }
}