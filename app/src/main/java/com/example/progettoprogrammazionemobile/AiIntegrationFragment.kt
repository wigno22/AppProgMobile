package com.example.progettoprogrammazionemobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class AiIntegrationFragment : Fragment() {

    private val apiKey = "cqpm661r01qmfvajvh1gcqpm661r01qmfvajvh20"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var investButton: Button
    private lateinit var showStocksButton: Button
    private lateinit var updateValuesButton: Button
    private lateinit var stockSelectionContainer: View
    private lateinit var investmentDataContainer: View
    private lateinit var investmentDataTextView: TextView
    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_aiintergration, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
        investButton = view.findViewById(R.id.button_invest)
        showStocksButton = view.findViewById(R.id.button_show_stocks)
        updateValuesButton = view.findViewById(R.id.button_update_values)
        stockSelectionContainer = view.findViewById(R.id.stock_selection_container)
        investmentDataContainer = view.findViewById(R.id.investment_data_container)
        investmentDataTextView = view.findViewById(R.id.investment_data)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = StockAdapter()
        recyclerView.adapter = adapter

        showStocksButton.setOnClickListener {
            stockSelectionContainer.visibility = View.VISIBLE
            investmentDataContainer.visibility = View.GONE
            showStocksButton.visibility = View.GONE
            updateValuesButton.visibility = View.GONE // Nascondi il bottone aggiorna valori
            lifecycleScope.launch {
                try {
                    fetchBestStocks()
                } catch (e: Exception) {
                    Log.e("AIIntegrationFragment", "Error fetching stocks", e)
                }
            }
        }

        investButton.setOnClickListener {
            val selectedStocks = adapter.getSelectedStocks()
            if (selectedStocks.isEmpty()) {
                Toast.makeText(requireContext(), "Please select stocks to invest in", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    saveInvestments(selectedStocks)
                    showInvestmentData()
                }
            }
        }

        updateValuesButton.setOnClickListener {
            lifecycleScope.launch {
                updateAllStockValues()
                showInvestmentData()
            }
        }

        // Chiamata per visualizzare le azioni esistenti all'avvio del fragment
        lifecycleScope.launch {
            showInvestmentData()
        }

        return view
    }

    private suspend fun fetchBestStocks() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(FinnhubApiService::class.java)
        try {
            val bestStocks = getBestStocks(service, apiKey)
            val randomStocks = bestStocks.shuffled().take(5)
            Log.d("AIIntegrationFragment", "Fetched best stocks: $randomStocks")
            adapter.submitList(randomStocks)
            investButton.visibility = View.VISIBLE // Assicurati che il bottone Invest sia visibile
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to fetch stocks: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to fetch stocks", e)
            }
        }
    }

    private suspend fun getStockSymbols(apiService: FinnhubApiService, apiKey: String): List<StockSymbol> {
        return withContext(Dispatchers.IO) {
            val response = apiService.getStockSymbols("US", apiKey).execute()

            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("AIIntegrationFragment", "Failed to get stock symbols: ${response.errorBody()?.string()}")
                emptyList()
            }
        }
    }

    private suspend fun getStockQuote(apiService: FinnhubApiService, apiKey: String, symbol: String): StockQuote? {
        return withContext(Dispatchers.IO) {
            val response = apiService.getStockQuote(symbol, apiKey).execute()
            if (response.isSuccessful) {
                response.body()

            } else {
                Log.e("AIIntegrationFragment", "Failed to get stock quote for $symbol: ${response.errorBody()?.string()}")
                null
            }
        }
    }

    private fun calculateRisk(quote: StockQuote): Double {
        return (quote.h - quote.l) / quote.c
    }

    private suspend fun getBestStocks(apiService: FinnhubApiService, apiKey: String): List<StockSymbolWithQuote> {
        val symbols = getStockSymbols(apiService, apiKey)


        val lowRiskStocks = mutableListOf<StockSymbolWithQuote>()
        var count = 0
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(java.util.Date())

        for (symbol in symbols) {
            if (count >= 40) {
                break
            }
            val quote = getStockQuote(apiService, apiKey, symbol.symbol)
            if (quote != null) {
                quote.valdata = currentDate
            }

            quote?.let {
                val risk = calculateRisk(it)
                if (risk < 0.1) {
                    lowRiskStocks.add(StockSymbolWithQuote(symbol, it))
                    count++
                }
            }
        }

        Log.d("AIIntegrationFragment", "Calculated low risk stocks: $lowRiskStocks")
        return lowRiskStocks
    }

    private suspend fun saveInvestments(stocks: List<StockSymbolWithQuote>) {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Azioni")
        val batch = db.batch()



        for (stock in stocks) {
            val quote = stock.quote
            val symbol = stock.symbol
            val docRef = userDocRef.document(symbol.symbol)
            val data = hashMapOf(
                "nomeAzione" to symbol.description,
                "valoreAcq" to quote.c,
                "dataAcq" to quote.valdata,
                "valoreUlt" to quote.c,
                "dataUlt" to quote.valdata,
                "valoreReal" to quote.c,
                "dataReal" to quote.valdata

            )
            batch.set(docRef, data)
        }

        try {
            batch.commit().await()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Investments saved successfully!", Toast.LENGTH_SHORT).show()
                investButton.visibility = View.GONE // Nascondi il bottone Invest dopo aver confermato
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to save investments: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to save investments", e)
            }
        }
    }

    private suspend fun updateAllStockValues() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(FinnhubApiService::class.java)
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Azioni")

        try {
            val snapshot = userDocRef.get().await()
            val batch = db.batch()

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(java.util.Date())

            for (document in snapshot.documents) {
                val symbol = document.id
                val quote = getStockQuote(service, apiKey, symbol)
                quote?.let {
                    val docRef = userDocRef.document(symbol)
                    batch.update(docRef, mapOf(
                        "valoreReal" to quote.c,
                        "dataReal" to currentDate
                    ))
                }
            }

            batch.commit().await()
            Toast.makeText(requireContext(), "Stock values updated successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to update stock values: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to update stock values", e)
            }
        }
    }

    private suspend fun showInvestmentData() {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Azioni")
        try {
            val snapshot = userDocRef.get().await()

            val investmentData = StringBuilder()
            for (document in snapshot.documents) {
                val data = document.data

                val stockName = data?.get("nomeAzione") as? String
                val purchaseAmount = data?.get("valoreAcq") as? Double
                val currentAmount = data?.get("valoreUlt") as?  Double
                val dateult= data?.get("dataUlt") as? String



                if (stockName != null) {
                    investmentData.append("Name: $stockName\n")
                    investmentData.append("Purchase Price: $purchaseAmount\n")
                    investmentData.append("Current Value: $currentAmount\n")
                    investmentData.append("Current Date: $dateult\n")
                }
            }
            investmentDataTextView.text = investmentData.toString()
            stockSelectionContainer.visibility = View.GONE
            investmentDataContainer.visibility = View.VISIBLE
            showStocksButton.visibility = View.VISIBLE
            updateValuesButton.visibility = View.VISIBLE // Mostra il bottone aggiorna valori
        } catch (e: Exception) {
            Log.e("AIIntegrationFragment", "Failed to fetch investment data", e)
            Toast.makeText(requireContext(), "Failed to fetch investment data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
