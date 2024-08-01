// InvestmentFragment.kt
package com.example.progettoprogrammazionemobile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.DecimalFormat
import java.util.*

class InvestmentFragment : Fragment() {

    private lateinit var saldoMedioTextView: TextView
    private lateinit var saldoCifraEditText: TextView
    private lateinit var cifraInvestimentoEditText: EditText
    private lateinit var radioGroupRischio: RadioGroup
    private lateinit var azioniCifraTextView: TextView
    private lateinit var fondiCifraTextView: TextView
    private lateinit var liquiditaCifraTextView: TextView
    private lateinit var confirmButton: Button
    private lateinit var periodoSpinner: Spinner
    private lateinit var previsioneRendimentoTextView: TextView
    private lateinit var azioniFattoreRischioTextView: TextView
    private lateinit var fondiFattoreRischioTextView: TextView
    private lateinit var liquiditaFattoreRischioTextView: TextView
    private lateinit var miglioriRecyclerView: RecyclerView // Aggiunto RecyclerView
    private lateinit var switchButton: Button // Aggiunto pulsante per cambiare visualizzazione

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var saldoMedio: Double = 0.0
    private var azioniFattoreRischio: Int = 0
    private var fondiFattoreRischio: Int = 0
    private var liquiditaFattoreRischio: Int = 0
    private var showingStocks = true // Stato della visualizzazione

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_investment, container, false)
        saldoMedioTextView = view.findViewById(R.id.saldo_medio)
        saldoCifraEditText = view.findViewById(R.id.saldo_cifra)
        cifraInvestimentoEditText = view.findViewById(R.id.cifra_investimento)
        radioGroupRischio = view.findViewById(R.id.radio_group_rischio)
        azioniCifraTextView = view.findViewById(R.id.azioni_cifra)
        fondiCifraTextView = view.findViewById(R.id.fondi_cifra)
        liquiditaCifraTextView = view.findViewById(R.id.liquidita_cifra)
        confirmButton = view.findViewById(R.id.confirm_button)
        periodoSpinner = view.findViewById(R.id.periodo_spinner)
        previsioneRendimentoTextView = view.findViewById(R.id.previsione_rendimento)
        azioniFattoreRischioTextView = view.findViewById(R.id.azioni_fattore_rischio)
        fondiFattoreRischioTextView = view.findViewById(R.id.fondi_fattore_rischio)
        liquiditaFattoreRischioTextView = view.findViewById(R.id.liquidita_fattore_rischio)
        miglioriRecyclerView = view.findViewById(R.id.migliori_recyclerview) // Inizializzare RecyclerView
        switchButton = view.findViewById(R.id.switch_button) // Inizializzare pulsante di switch

        setupPeriodoSpinner()
        setupRecyclerView()
        fetchSaldoMedioTrimestrale()
        fetchMiglioriAzioni()
        confirmButton.setOnClickListener { onConfirmButtonClick() }
        switchButton.setOnClickListener { onSwitchButtonClick() } // Impostare listener per il pulsante di switch

        return view
    }

    private fun setupPeriodoSpinner() {
        val periodi = arrayOf("6 mesi", "12 mesi", "18 mesi", "24 mesi", "36 mesi")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periodi)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodoSpinner.adapter = adapter
    }

    private fun setupRecyclerView() {
        miglioriRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun fetchSaldoMedioTrimestrale() {
        val user = auth.currentUser
        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            val threeMonthsAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -3)
            }.time

            val transactionsRef = userDocRef.collection("Transaction")
                .whereGreaterThan("date", threeMonthsAgo)
                .orderBy("date", Query.Direction.ASCENDING)

            transactionsRef.get().addOnSuccessListener { querySnapshot ->
                var saldoTotale = 0.0

                Log.d("InvestimentiFragment", "Number of documents: ${querySnapshot.documents.size}")

                for (document in querySnapshot.documents) {
                    var amount = document.getDouble("amount") ?: 0.0
                    val isOutgoing = document.getBoolean("outgoing") ?: false
                    if (isOutgoing) amount *= -1 else amount *= 1

                    saldoTotale += amount

                    Log.d("InvestimentiFragment", "Document: $document, Amount: $amount, Running Total: $saldoTotale")
                }

                saldoMedio = saldoTotale / 3
                val decimalFormat = DecimalFormat("#.0")
                val saldoMedioFormatted = decimalFormat.format(saldoMedio)

                saldoCifraEditText.setText(saldoMedioFormatted)

                Log.d("InvestimentiFragment", "Saldo medio: $saldoMedioFormatted")
            }.addOnFailureListener { exception ->
                Log.e("InvestimentiFragment", "Failed to fetch transactions: ${exception.message}")
            }
        } else {
            Log.e("InvestimentiFragment", "User ID is null")
        }
    }

    private fun fetchMiglioriAzioni() {
        val apiKey = "KMD5G7J3GYUXUOLA" // Inserisci la tua chiave API Alpha Vantage
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=IBM&interval=5min&apikey=$apiKey")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("InvestmentFragment", "Failed to fetch migliori azioni: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val json = responseBody.string()
                        Log.d("InvestmentFragment", "API Response: $json")

                        val jsonObject = JSONObject(json)
                        val timeSeries = jsonObject.getJSONObject("Time Series (5min)")
                        val keys = timeSeries.keys()
                        val itemList = mutableListOf<MiglioreItem>()

                        var count = 0
                        while (keys.hasNext() && count < 5) {
                            val key = keys.next()
                            val data = timeSeries.getJSONObject(key)
                            val name = "IBM"
                            val value = data.getString("4. close")
                            itemList.add(MiglioreItem(name, value))
                            count++
                        }

                        requireActivity().runOnUiThread {
                           // Log.d("InvestmentFragment", "Updating RecyclerView with ${itemList.size} items")
                            miglioriRecyclerView.adapter = MiglioriAdapter(itemList)
                        }
                    }
                } else {
                    Log.e("InvestmentFragment", "API Response not successful: ${response.code}")
                }
            }
        })
    }



    private fun fetchMiglioriFondi() {
        val apiKey = "cqlpqj1r01qoqqs7o4c0cqlpqj1r01qoqqs7o4cg" // Inserisci la tua chiave API Finnhub
        val client = OkHttpClient()

        // Modifica l'URL per utilizzare l'endpoint che fornisce una lista di fondi o aziende
        val request = Request.Builder()
            .url("https://finnhub.io/api/v1/stock/symbol?exchange=US&token=$apiKey") // Esempio di endpoint, sostituisci con quello corretto
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("InvestmentFragment", "Failed to fetch data: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val json = responseBody.string()
                        Log.d("InvestmentFragment", "API Response: $json")

                        try {
                            val jsonArray = JSONArray(json)
                            val itemList = mutableListOf<MiglioreItem>()

                            // Limita il numero di elementi a 5
                            val limit = minOf(5, jsonArray.length())
                            for (i in 0 until limit) {
                                val item = jsonArray.getJSONObject(i)
                                val name = item.getString("description") // Usa la chiave corretta per il nome del fondo o azienda
                                val symbol = item.getString("symbol") // Usa la chiave corretta per il simbolo

                                itemList.add(MiglioreItem(name, symbol))
                            }

                            requireActivity().runOnUiThread {
                                Log.d("InvestmentFragment", "Updating RecyclerView with ${itemList.size} items")
                                miglioriRecyclerView.adapter = MiglioriAdapter(itemList)
                            }
                        } catch (e: JSONException) {
                            Log.e("InvestmentFragment", "Errore durante il parsing del JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("InvestmentFragment", "API Response not successful: ${response.code}")
                }
            }
        })
    }





    private fun onConfirmButtonClick() {
        val cifraInvestimento = cifraInvestimentoEditText.text.toString().toDoubleOrNull() ?: return
        val selectedPeriodo = periodoSpinner.selectedItem.toString()
        val periodo = selectedPeriodo.split(" ")[0].toInt()

        val rischioId = radioGroupRischio.checkedRadioButtonId
        val fattoreRischio = when (rischioId) {
            R.id.rischio_basso -> 1
            R.id.rischio_medio -> 2
            R.id.rischio_alto -> 3
            else -> 1
        }

        val rendimentoPrevisto = calcolaRendimentoPrevisto(cifraInvestimento, periodo, fattoreRischio)
        previsioneRendimentoTextView.text = rendimentoPrevisto.toString()
    }

    private fun onSwitchButtonClick() {
        showingStocks = !showingStocks
        if (showingStocks) {
            fetchMiglioriAzioni()
            switchButton.text = "Mostra Fondi"
        } else {
            fetchMiglioriFondi()
            switchButton.text = "Mostra Azioni"
        }
    }

    private fun calcolaRendimentoPrevisto(cifra: Double, periodo: Int, rischio: Int): Double {
        // Placeholder per il calcolo effettivo del rendimento previsto
        return cifra * periodo * rischio / 100.0
    }
}
