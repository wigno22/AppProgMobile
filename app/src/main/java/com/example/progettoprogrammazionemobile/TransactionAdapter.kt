package com.example.progettoprogrammazionemobile


import android.content.Context
import android.graphics.Color
import android.icu.text.NumberFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.Locale

data class Transaction(val date: String, val type: String, val amount: Double, val category: String)

class TransactionAdapter(private val context: Context, private val transactions: List<Transaction>) : BaseAdapter() {

    override fun getCount(): Int {
        return transactions.size
    }

    override fun getItem(position: Int): Any {
        return transactions[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    //per tutte le transazioni presenti nella lista:
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        try {
            view = convertView ?: LayoutInflater.from(context).inflate(R.layout.transactiongrid, parent, false)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        val transaction = transactions[position]

        //recupero tutti i dati necessari dagli elementi xml
        val dateTextView = view.findViewById<TextView>(R.id.transactionDate)
        val typeTextView = view.findViewById<TextView>(R.id.transactionType)
        val amountTextView = view.findViewById<TextView>(R.id.transactionAmount)
        val categoryTextView = view.findViewById<TextView>(R.id.transactionCategory)

        dateTextView.text = transaction.date
        typeTextView.text = transaction.type
        amountTextView.text = transaction.amount.toString()
        categoryTextView.text = transaction.category

        val numberFormat = NumberFormat.getNumberInstance(Locale.ITALY)
        val formattedAmount = numberFormat.format(transaction.amount)

        // cambio colore al valore
        if (transaction.amount < 0) {
            amountTextView.setTextColor(Color.RED)
        } else {
            amountTextView.setTextColor(ContextCompat.getColor(context, R.color.verdescuro))
        }

        amountTextView.text = formattedAmount
        return view
    }
}
