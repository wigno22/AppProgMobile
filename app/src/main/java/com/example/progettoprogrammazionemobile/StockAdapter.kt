package com.example.progettoprogrammazionemobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView


class StockAdapter : ListAdapter<StockSymbolWithQuote, StockAdapter.StockViewHolder>(StockDiffCallback()) {

    private val selectedStocks = mutableSetOf<StockSymbolWithQuote>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.stock_item, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = getItem(position)
        holder.bind(stock)
    }

    fun getSelectedStocks(): List<StockSymbolWithQuote> = selectedStocks.toList()

    inner class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.text_action_name)
        private val textCurrentValue: TextView = itemView.findViewById(R.id.text_current_value)
        private val textMinValue: TextView = itemView.findViewById(R.id.text_min_value)
        private val textMaxValue: TextView = itemView.findViewById(R.id.text_max_value)
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_select)

        fun bind(stock: StockSymbolWithQuote) {
            textName.text = stock.symbol.symbol
            textCurrentValue.text = "Valore Attuale: ${stock.quote.c}"
            textMinValue.text = "Minimo: ${stock.quote.l}"
            textMaxValue.text = "Massimo: ${stock.quote.h}"

            // Placeholder for date, which will be set when the item is selected
            textDate.text = "Data: Not Set"

            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedStocks.add(stock)
                } else {
                    selectedStocks.remove(stock)
                }
            }
        }
    }
}

class StockDiffCallback : DiffUtil.ItemCallback<StockSymbolWithQuote>() {
    override fun areItemsTheSame(oldItem: StockSymbolWithQuote, newItem: StockSymbolWithQuote): Boolean {
        return oldItem.symbol.symbol == newItem.symbol.symbol
    }

    override fun areContentsTheSame(oldItem: StockSymbolWithQuote, newItem: StockSymbolWithQuote): Boolean {
        return oldItem == newItem
    }
}


data class StockSymbolWithQuote(
    val symbol: StockSymbol,
    val quote: StockQuote
)

data class StockSymbol(
    val symbol: String,
    val description: String
)

data class StockQuote(
    val c: Double,  // Prezzo corrente
    val h: Double,  // Prezzo massimo del giorno
    val l: Double,  // Prezzo minimo del giorno
    val pc: Double  // Prezzo di chiusura precedente
)
