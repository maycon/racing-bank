package com.hacknroll.racing_bank.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.data.models.TransactionItem
import com.hacknroll.racing_bank.data.models.TransferItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

sealed class StatementItem {
    data class Transaction(val item: TransactionItem) : StatementItem()
    data class Transfer(val item: TransferItem) : StatementItem()
}

class StatementAdapter(
    private val onItemClick: (StatementItem) -> Unit = {}
) : ListAdapter<StatementItem, StatementAdapter.ViewHolder>(StatementDiffCallback()) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.transactionIcon)
        private val typeText: TextView = itemView.findViewById(R.id.transactionTypeText)
        private val descriptionText: TextView = itemView.findViewById(R.id.transactionDescriptionText)
        private val dateText: TextView = itemView.findViewById(R.id.transactionDateText)
        private val amountText: TextView = itemView.findViewById(R.id.transactionAmountText)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(adapterPosition))
                }
            }
        }

        fun bind(item: StatementItem) {
            when (item) {
                is StatementItem.Transaction -> bindTransaction(item.item)
                is StatementItem.Transfer -> bindTransfer(item.item)
            }
        }

        private fun bindTransaction(transaction: TransactionItem) {
            val context = itemView.context

            typeText.text = transaction.type.uppercase()
            descriptionText.text = transaction.description ?: "Transaction"

            // Parse and format date
            try {
                val parsedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    .parse(transaction.createdAt)
                dateText.text = parsedDate?.let { dateFormatter.format(it) } ?: transaction.createdAt
            } catch (e: Exception) {
                dateText.text = transaction.createdAt
            }

            when (transaction.type.lowercase()) {
                "deposit" -> {
                    icon.setImageResource(R.drawable.ic_money)
                    icon.setColorFilter(context.getColor(R.color.retro_green))
                    amountText.text = "+${currencyFormatter.format(transaction.amount)}"
                    amountText.setTextColor(context.getColor(R.color.retro_green))
                }

                "withdrawal" -> {
                    icon.setImageResource(R.drawable.ic_money)
                    icon.setColorFilter(context.getColor(R.color.retro_red))
                    amountText.text = "-${currencyFormatter.format(transaction.amount)}"
                    amountText.setTextColor(context.getColor(R.color.retro_red))
                }

                "subscription" -> {
                    icon.setImageResource(R.drawable.ic_arrow_right)
                    icon.setColorFilter(context.getColor(R.color.retro_yellow))
                    amountText.text = "-${currencyFormatter.format(transaction.amount)}"
                    amountText.setTextColor(context.getColor(R.color.retro_yellow))
                }

                "redemption" -> {
                    icon.setImageResource(R.drawable.ic_arrow_right)
                    icon.setColorFilter(context.getColor(R.color.retro_cyan))
                    amountText.text = "+${currencyFormatter.format(transaction.amount)}"
                    amountText.setTextColor(context.getColor(R.color.retro_cyan))
                }

                else -> {
                    icon.setImageResource(R.drawable.ic_money)
                    icon.setColorFilter(context.getColor(R.color.retro_gray))
                    amountText.text = currencyFormatter.format(transaction.amount)
                    amountText.setTextColor(context.getColor(R.color.retro_white))
                }
            }
        }

        private fun bindTransfer(transfer: TransferItem) {
            val context = itemView.context
            val currentUsername = context.getSharedPreferences("session", 0)
                .getString("username", "") ?: ""

            icon.setImageResource(R.drawable.ic_person)

            if (transfer.fromUsername == currentUsername) {
                // Outgoing transfer
                typeText.text = "TRANSFER OUT"
                descriptionText.text = "To: ${transfer.toUsername}"
                amountText.text = "-${currencyFormatter.format(transfer.amount)}"
                amountText.setTextColor(context.getColor(R.color.retro_red))
                icon.setColorFilter(context.getColor(R.color.retro_red))
            } else {
                // Incoming transfer
                typeText.text = "TRANSFER IN"
                descriptionText.text = "From: ${transfer.fromUsername}"
                amountText.text = "+${currencyFormatter.format(transfer.amount)}"
                amountText.setTextColor(context.getColor(R.color.retro_green))
                icon.setColorFilter(context.getColor(R.color.retro_green))
            }

            // Parse and format date
            try {
                val parsedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    .parse(transfer.createdAt)
                dateText.text = parsedDate?.let { dateFormatter.format(it) } ?: transfer.createdAt
            } catch (e: Exception) {
                dateText.text = transfer.createdAt
            }
        }
    }

    class StatementDiffCallback : DiffUtil.ItemCallback<StatementItem>() {
        override fun areItemsTheSame(oldItem: StatementItem, newItem: StatementItem): Boolean {
            return when {
                oldItem is StatementItem.Transaction && newItem is StatementItem.Transaction ->
                    oldItem.item.id == newItem.item.id

                oldItem is StatementItem.Transfer && newItem is StatementItem.Transfer ->
                    oldItem.item.id == newItem.item.id

                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: StatementItem, newItem: StatementItem): Boolean {
            return oldItem == newItem
        }
    }
}