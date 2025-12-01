package com.hacknroll.racing_bank.ui.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.data.models.StatementResponse
import com.hacknroll.racing_bank.data.models.TransactionItem
import com.hacknroll.racing_bank.data.models.TransferItem
import com.hacknroll.racing_bank.ui.main.MainViewModel
import com.hacknroll.racing_bank.utils.Resource
import com.hacknroll.racing_bank.utils.SessionManager
import com.hacknroll.racing_bank.utils.SoundManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class StatementFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var sessionManager: SessionManager

    // Views
    private lateinit var cashBalanceText: TextView
    private lateinit var investmentValueText: TextView
    private lateinit var totalPortfolioText: TextView
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipTransactions: Chip
    private lateinit var chipTransfers: Chip
    private lateinit var refreshButton: ImageButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var statementRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var statementAdapter: StatementAdapter
    private var currentFilter = FilterType.ALL
    private var statementData: StatementResponse? = null

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    enum class FilterType {
        ALL, TRANSACTIONS, TRANSFERS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        sessionManager = SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Load initial data
        loadStatement()
    }

    private fun initViews(view: View) {
        cashBalanceText = view.findViewById(R.id.cashBalanceText)
        investmentValueText = view.findViewById(R.id.investmentValueText)
        totalPortfolioText = view.findViewById(R.id.totalPortfolioText)
        filterChipGroup = view.findViewById(R.id.filterChipGroup)
        chipAll = view.findViewById(R.id.chipAll)
        chipTransactions = view.findViewById(R.id.chipTransactions)
        chipTransfers = view.findViewById(R.id.chipTransfers)
        refreshButton = view.findViewById(R.id.refreshButton)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        statementRecyclerView = view.findViewById(R.id.statementRecyclerView)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        progressBar = view.findViewById(R.id.progressBar)

        applyRetroStyling()
    }

    private fun applyRetroStyling() {
        swipeRefreshLayout.setColorSchemeColors(
            resources.getColor(R.color.retro_cyan, null),
            resources.getColor(R.color.retro_pink, null),
            resources.getColor(R.color.retro_yellow, null)
        )
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            resources.getColor(R.color.retro_dark_blue, null)
        )
    }

    private fun setupRecyclerView() {
        statementAdapter = StatementAdapter()
        statementRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        statementRecyclerView.adapter = statementAdapter
    }

    private fun setupListeners() {
        filterChipGroup.setOnCheckedChangeListener { _, checkedId ->
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            when (checkedId) {
                R.id.chipAll -> {
                    currentFilter = FilterType.ALL
                    applyFilter()
                }

                R.id.chipTransactions -> {
                    currentFilter = FilterType.TRANSACTIONS
                    applyFilter()
                }

                R.id.chipTransfers -> {
                    currentFilter = FilterType.TRANSFERS
                    applyFilter()
                }
            }
        }

        refreshButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            loadStatement()
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadStatement()
        }
    }

    private fun loadStatement() {
        viewModel.loadStatement()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statementState.collectLatest { resource ->
                when (resource) {
                    is Resource.Idle -> {
                        // Initial state - do nothing
                    }

                    is Resource.Loading -> {
                        showLoading(true)
                    }

                    is Resource.Success -> {
                        showLoading(false)
                        statementData = resource.data
                        updateBalances(resource.data)
                        applyFilter()
                    }

                    is Resource.Error -> {
                        showLoading(false)
                        showError(resource.message)
                    }
                }
            }
        }
    }

    private fun updateBalances(statement: StatementResponse) {
        cashBalanceText.text = currencyFormatter.format(statement.cashBalance)
        investmentValueText.text = currencyFormatter.format(statement.fundValue)

        val totalPortfolio = statement.cashBalance + statement.fundValue
        totalPortfolioText.text = currencyFormatter.format(totalPortfolio)

        // Update color based on value
        cashBalanceText.setTextColor(
            if (statement.cashBalance > 0)
                resources.getColor(R.color.retro_green, null)
            else
                resources.getColor(R.color.retro_red, null)
        )

        investmentValueText.setTextColor(
            if (statement.fundValue > 0)
                resources.getColor(R.color.retro_yellow, null)
            else
                resources.getColor(R.color.retro_gray, null)
        )
    }

    private fun applyFilter() {
        val items = mutableListOf<StatementListItem>()

        statementData?.let { statement ->
            when (currentFilter) {
                FilterType.ALL -> {
                    // Combine and sort all items by date
                    statement.transactions.forEach { items.add(StatementListItem.Transaction(it)) }
                    statement.transfers.forEach { items.add(StatementListItem.Transfer(it)) }
                    items.sortByDescending { item ->
                        when (item) {
                            is StatementListItem.Transaction -> item.transaction.createdAt
                            is StatementListItem.Transfer -> item.transfer.createdAt
                        }
                    }
                }

                FilterType.TRANSACTIONS -> {
                    statement.transactions.forEach { items.add(StatementListItem.Transaction(it)) }
                }

                FilterType.TRANSFERS -> {
                    statement.transfers.forEach { items.add(StatementListItem.Transfer(it)) }
                }
            }
        }

        statementAdapter.submitList(items)

        // Show empty state if no items
        if (items.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            statementRecyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            statementRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        swipeRefreshLayout.isRefreshing = show
        progressBar.visibility = if (show && statementData == null) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        view?.let {
            val snackbar = Snackbar.make(it, message, Snackbar.LENGTH_LONG)
            snackbar.setBackgroundTint(resources.getColor(R.color.retro_red, null))
            snackbar.setTextColor(resources.getColor(R.color.retro_white, null))
            snackbar.show()
        }
        SoundManager.playSound(SoundManager.SoundType.ERROR)
    }

    // Statement List Item
    sealed class StatementListItem {
        data class Transaction(val transaction: TransactionItem) : StatementListItem()
        data class Transfer(val transfer: TransferItem) : StatementListItem()
    }

    // Statement Adapter
    inner class StatementAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val TYPE_TRANSACTION = 0
        private val TYPE_TRANSFER = 1
        private val items = mutableListOf<StatementListItem>()
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

        fun submitList(newItems: List<StatementListItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is StatementListItem.Transaction -> TYPE_TRANSACTION
                is StatementListItem.Transfer -> TYPE_TRANSFER
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_TRANSACTION -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_transaction, parent, false)
                    TransactionViewHolder(view)
                }

                TYPE_TRANSFER -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_transfer, parent, false)
                    TransferViewHolder(view)
                }

                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is StatementListItem.Transaction -> {
                    (holder as TransactionViewHolder).bind(item.transaction)
                }

                is StatementListItem.Transfer -> {
                    (holder as TransferViewHolder).bind(item.transfer)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        // Transaction ViewHolder
        inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val typeIcon: ImageView = itemView.findViewById(R.id.transactionIcon)
            private val typeText: TextView = itemView.findViewById(R.id.transactionTypeText)
            private val amountText: TextView = itemView.findViewById(R.id.transactionAmountText)
            private val dateText: TextView = itemView.findViewById(R.id.transactionDateText)
            private val descriptionText: TextView = itemView.findViewById(R.id.transactionDescriptionText)

            fun bind(transaction: TransactionItem) {
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

                // Set icon and color based on type
                when (transaction.type.lowercase()) {
                    "deposit" -> {
                        typeIcon.setImageResource(R.drawable.ic_money)
                        typeIcon.setColorFilter(itemView.context.getColor(R.color.retro_green))
                        amountText.text = "+${currencyFormatter.format(transaction.amount)}"
                        amountText.setTextColor(itemView.context.getColor(R.color.retro_green))
                    }

                    "withdrawal" -> {
                        typeIcon.setImageResource(R.drawable.ic_money)
                        typeIcon.setColorFilter(itemView.context.getColor(R.color.retro_red))
                        amountText.text = "-${currencyFormatter.format(transaction.amount)}"
                        amountText.setTextColor(itemView.context.getColor(R.color.retro_red))
                    }

                    "subscription" -> {
                        typeIcon.setImageResource(R.drawable.ic_investment)
                        typeIcon.setColorFilter(itemView.context.getColor(R.color.retro_yellow))
                        amountText.text = "-${currencyFormatter.format(transaction.amount)}"
                        amountText.setTextColor(itemView.context.getColor(R.color.retro_yellow))
                    }

                    "redemption" -> {
                        typeIcon.setImageResource(R.drawable.ic_investment)
                        typeIcon.setColorFilter(itemView.context.getColor(R.color.retro_cyan))
                        amountText.text = "+${currencyFormatter.format(transaction.amount)}"
                        amountText.setTextColor(itemView.context.getColor(R.color.retro_cyan))
                    }

                    else -> {
                        typeIcon.setImageResource(R.drawable.ic_money)
                        typeIcon.setColorFilter(itemView.context.getColor(R.color.retro_gray))
                        amountText.text = currencyFormatter.format(transaction.amount)
                        amountText.setTextColor(itemView.context.getColor(R.color.retro_white))
                    }
                }
            }
        }

        // Transfer ViewHolder
        inner class TransferViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val typeIcon: ImageView = itemView.findViewById(R.id.typeIcon)
            private val fromText: TextView = itemView.findViewById(R.id.fromText)
            private val toText: TextView = itemView.findViewById(R.id.toText)
            private val amountText: TextView = itemView.findViewById(R.id.amountText)
            private val dateText: TextView = itemView.findViewById(R.id.dateText)

            fun bind(transfer: TransferItem) {
                val currentUsername = sessionManager.getUsername() ?: ""

                typeIcon.setImageResource(R.drawable.ic_transfer)

                if (transfer.fromUsername == currentUsername) {
                    // Outgoing transfer
                    fromText.text = "From: You"
                    toText.text = "To: ${transfer.toUsername}"
                    amountText.text = "-${currencyFormatter.format(transfer.amount)}"
                    amountText.setTextColor(itemView.context.getColor(R.color.retro_red))
                    typeIcon.setColorFilter(itemView.context.getColor(R.color.retro_red))
                } else {
                    // Incoming transfer
                    fromText.text = "From: ${transfer.fromUsername}"
                    toText.text = "To: You"
                    amountText.text = "+${currencyFormatter.format(transfer.amount)}"
                    amountText.setTextColor(itemView.context.getColor(R.color.retro_green))
                    typeIcon.setColorFilter(itemView.context.getColor(R.color.retro_green))
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
    }
}