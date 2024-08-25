package com.example.progettoprogrammazionemobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CryptoAdapter : ListAdapter<CryptoSymbolWithQuote, CryptoAdapter.CryptoViewHolder>(CryptoDiffCallback()) {

    private val selectedCryptos = mutableSetOf<CryptoSymbolWithQuote>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CryptoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.crypto_item, parent, false)
        return CryptoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CryptoViewHolder, position: Int) {
        val crypto = getItem(position)
        holder.bind(crypto)
    }

    fun getSelectedCrypto(): List<CryptoSymbolWithQuote> = selectedCryptos.toList()

    inner class CryptoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textSymbol: TextView = itemView.findViewById(R.id.text_crypto_symbol)
        private val textName: TextView = itemView.findViewById(R.id.text_crypto_name)
        private val textCurrentValue: TextView = itemView.findViewById(R.id.text_current_value)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_select)

        fun bind(crypto: CryptoSymbolWithQuote) {
            textSymbol.text = crypto.symbol.symbol
            textName.text = crypto.symbol.name
            textCurrentValue.text = "Current Value: ${crypto.quote.price} $"

            checkbox.setOnCheckedChangeListener(null) // Rimuove l'eventuale listener precedente
            checkbox.isChecked = selectedCryptos.contains(crypto)

            // Imposta un listener per la CheckBox senza causare NullPointerException
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedCryptos.add(crypto)
                } else {
                    selectedCryptos.remove(crypto)
                }
            }
        }
    }
}

class CryptoDiffCallback : DiffUtil.ItemCallback<CryptoSymbolWithQuote>() {
    override fun areItemsTheSame(oldItem: CryptoSymbolWithQuote, newItem: CryptoSymbolWithQuote): Boolean {
        return oldItem.symbol.symbol == newItem.symbol.symbol
    }

    override fun areContentsTheSame(oldItem: CryptoSymbolWithQuote, newItem: CryptoSymbolWithQuote): Boolean {
        return oldItem == newItem
    }
}
