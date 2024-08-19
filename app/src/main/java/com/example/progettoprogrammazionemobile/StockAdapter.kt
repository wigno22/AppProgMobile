package com.example.progettoprogrammazionemobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class StockAndFundAdapter : ListAdapter<Any, RecyclerView.ViewHolder>(StockAndFundDiffCallback()) {

    private val selectedItems = mutableSetOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.stock_item, parent, false)
        return if (viewType == VIEW_TYPE_STOCK) {
            StockViewHolder(view)
        } else {
            FundViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is StockViewHolder && item is StockSymbolWithQuote) {
            holder.bind(item)
        } else if (holder is FundViewHolder && item is FundSymbolWithQuote) {
            holder.bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is StockSymbolWithQuote) {
            VIEW_TYPE_STOCK
        } else {
            VIEW_TYPE_FUND
        }
    }

    fun getSelectedStocks(): List<StockSymbolWithQuote> {
        return selectedItems.filterIsInstance<StockSymbolWithQuote>()
    }

    fun getSelectedFunds(): List<FundSymbolWithQuote> {
        return selectedItems.filterIsInstance<FundSymbolWithQuote>()
    }

    inner class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.text_action_name)
        private val textCurrentValue: TextView = itemView.findViewById(R.id.text_current_value)
        private val description: TextView = itemView.findViewById(R.id.text_description)
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_select)

        fun bind(stock: StockSymbolWithQuote) {
            textName.text = stock.symbol.symbol
            textCurrentValue.text = "Valore Attuale: ${stock.quote.c}"
            description.text = stock.symbol.description
            textDate.text = stock.quote.valdata

            checkbox.isChecked = selectedItems.contains(stock)
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(stock)
                } else {
                    selectedItems.remove(stock)
                }
            }
        }
    }

    inner class FundViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.text_action_name)
        private val textCurrentValue: TextView = itemView.findViewById(R.id.text_current_value)
        private val description: TextView = itemView.findViewById(R.id.text_description)
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_select)

        fun bind(fund: FundSymbolWithQuote) {
            textName.text = fund.symbol.symbol
            textCurrentValue.text = "Valore Attuale: ${fund.quote.c}"
            description.text = fund.symbol.description
            textDate.text = fund.quote.valdata

            checkbox.isChecked = selectedItems.contains(fund)
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(fund)
                } else {
                    selectedItems.remove(fund)
                }
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_STOCK = 1
        private const val VIEW_TYPE_FUND = 2
    }
}

class StockAndFundDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is StockSymbolWithQuote && newItem is StockSymbolWithQuote -> oldItem.symbol.symbol == newItem.symbol.symbol
            oldItem is FundSymbolWithQuote && newItem is FundSymbolWithQuote -> oldItem.symbol.symbol == newItem.symbol.symbol
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem
    }
}
