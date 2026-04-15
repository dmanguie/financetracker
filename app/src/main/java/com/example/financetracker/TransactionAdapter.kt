package com.example.financetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onEdit: (Transaction) -> Unit,
    private val onDelete: (Long) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tvEmoji)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = transactions[position]

        holder.tvEmoji.text = getCategoryEmoji(t.category)
        holder.tvTitle.text = t.title
        holder.tvCategory.text = t.category
        holder.tvDate.text = t.date

        if (t.type == "Income") {
            holder.tvAmount.text = "+₱${String.format("%,.2f", t.amount)}"
            holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.income_green))
            holder.tvType.text = "INCOME"
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_income)
            holder.tvType.setTextColor(holder.itemView.context.getColor(R.color.income_green))
        } else {
            holder.tvAmount.text = "-₱${String.format("%,.2f", t.amount)}"
            holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.expense_red))
            holder.tvType.text = "EXPENSE"
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_expense)
            holder.tvType.setTextColor(holder.itemView.context.getColor(R.color.expense_red))
        }

        holder.btnEdit.setOnClickListener { onEdit(t) }
        holder.btnDelete.setOnClickListener { onDelete(t.id) }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }

    companion object {
        fun getCategoryEmoji(category: String): String = when (category) {
            "Food" -> "🍜"
            "Transport" -> "🚌"
            "School" -> "📚"
            "Entertainment" -> "🎮"
            "Health" -> "💊"
            "Shopping" -> "🛍️"
            "Allowance" -> "💸"
            "Part-time" -> "💼"
            "Savings" -> "🏦"
            else -> "📝"
        }
    }
}