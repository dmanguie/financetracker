package com.example.financetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onEdit: (Transaction) -> Unit,
    private val onDelete: (Long) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DIFF_CALLBACK) {

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
        val t = getItem(position)
        val ctx = holder.itemView.context

        holder.tvEmoji.text = getCategoryEmoji(t.category)
        holder.tvTitle.text = t.title
        holder.tvCategory.text = t.category
        holder.tvDate.text = formatDate(t.dateMillis)

        if (t.type == TransactionViewModel.TYPE_INCOME) {
            holder.tvAmount.text = ctx.getString(R.string.amount_income, t.amount)
            holder.tvAmount.setTextColor(ctx.getColor(R.color.income_green))
            holder.tvType.text = ctx.getString(R.string.label_income)
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_income)
            holder.tvType.setTextColor(ctx.getColor(R.color.income_green))
        } else {
            holder.tvAmount.text = ctx.getString(R.string.amount_expense, t.amount)
            holder.tvAmount.setTextColor(ctx.getColor(R.color.expense_red))
            holder.tvType.text = ctx.getString(R.string.label_expense)
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_expense)
            holder.tvType.setTextColor(ctx.getColor(R.color.expense_red))
        }

        holder.btnEdit.setOnClickListener { onEdit(t) }
        holder.btnDelete.setOnClickListener { onDelete(t.id) }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(old: Transaction, new: Transaction) = old.id == new.id
            override fun areContentsTheSame(old: Transaction, new: Transaction) = old == new
        }

        private val displaySdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fun formatDate(millis: Long): String = displaySdf.format(Date(millis))

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