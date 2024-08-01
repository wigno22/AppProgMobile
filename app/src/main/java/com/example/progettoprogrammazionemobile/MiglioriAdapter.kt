// MiglioriAdapter.kt
package com.example.progettoprogrammazionemobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MiglioriAdapter(private val itemList: List<MiglioreItem>) : RecyclerView.Adapter<MiglioriAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemTextView: TextView = itemView.findViewById(R.id.item_text)
        val itemValueTextView: TextView = itemView.findViewById(R.id.item_value) // Aggiunta nuova TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_migliore, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.itemTextView.text = item.name
        holder.itemValueTextView.text = item.value // Imposta il valore
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}

data class MiglioreItem(val name: String, val value: String) // Aggiunto modello di dati
