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
import android.widget.ProgressBar
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class StockFragment : Fragment() {

    private val apiKey = "cqrngv9r01quefaheobgcqrngv9r01quefaheoc0"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var investButton: Button
    private lateinit var showStocksButton: Button
    private lateinit var updateValuesButton: Button
    private lateinit var sellAllButton: Button
    private lateinit var stockSelectionContainer: View
    private lateinit var investmentDataContainer: View
    private lateinit var investmentDataTextView: TextView
    private lateinit var progressBar: ProgressBar
    private var exchangeRate: Double = 1.09
    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stock, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
        investButton = view.findViewById(R.id.button_invest)
        showStocksButton = view.findViewById(R.id.button_show_stocks)
        updateValuesButton = view.findViewById(R.id.button_update_values)
        sellAllButton = view.findViewById(R.id.button_sellall)
        stockSelectionContainer = view.findViewById(R.id.stock_selection_container)
        investmentDataContainer = view.findViewById(R.id.investment_data_container)
        investmentDataTextView = view.findViewById(R.id.investment_data)
        progressBar = view.findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = StockAdapter()
        recyclerView.adapter = adapter

        showStocksButton.setOnClickListener {
            stockSelectionContainer.visibility = View.VISIBLE
            investmentDataContainer.visibility = View.GONE
            showStocksButton.visibility = View.GONE
            updateValuesButton.visibility = View.GONE
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
                    registerInvestmentTransaction(selectedStocks)
                    showInvestmentData()
                    updateAllStockValues()
                }
            }
        }

        updateValuesButton.setOnClickListener {
            lifecycleScope.launch {
                updateAllStockValues()
                showInvestmentData()
            }
        }

        sellAllButton.setOnClickListener {
            lifecycleScope.launch {
                sellAllStocks()
            }
        }

        // Chiamata per visualizzare le azioni esistenti all'avvio del fragment
        lifecycleScope.launch {
            showInvestmentData()
        }

        return view
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://finnhub.io/api/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(FinnhubApiService::class.java)

    private suspend fun showInvestmentData() {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Azioni")
        try {
            val snapshot = userDocRef.get().await()

            if (snapshot.isEmpty) {
               // Se non ci sono azioni, carica le migliori azioni disponibili automaticamente
                showStocksButton.visibility = View.VISIBLE // Nascondi il pulsante Visualizza Azioni se non ci sono azioni
                return
            }

            val investmentData = StringBuilder()
            for (document in snapshot.documents) {
                val data = document.data

                val stockName = data?.get("nomeAzione") as? String
                val purchaseAmount = data?.get("valoreAcq$") as? Double
                val realAmount = data?.get("valoreReal$") as? Double
                val currentAmount = data?.get("valoreUlt$") as? Double
                val dateUlt = data?.get("dataUlt") as? String
                val numStocks = data?.get("numeroAzioni") as? Double

                val numberFormat = NumberFormat.getCurrencyInstance(Locale.ITALY)

                if (stockName != null) {
                    investmentData.append("Name: $stockName\n")
                    investmentData.append("Price\n")
                    investmentData.append("Purchase: ${purchaseAmount?.let { numberFormat.format(it) } ?: "N/A"}\n")
                    investmentData.append("Last: ${realAmount?.let { numberFormat.format(it) } ?: "N/A"}\n")
                    investmentData.append("Current: ${currentAmount?.let { numberFormat.format(it) } ?: "N/A"}\n")
                    investmentData.append("Current Date: ${dateUlt ?: "N/A"}\n")
                    investmentData.append("Number of Stocks: ${numStocks ?: "N/A"}\n")
                    investmentData.append("\n")
                }
            }

            investmentDataTextView.text = investmentData.toString()
            stockSelectionContainer.visibility = View.GONE
            investmentDataContainer.visibility = View.VISIBLE
            showStocksButton.visibility = View.GONE // Nascondi il pulsante Visualizza Azioni se ci sono già azioni
            updateValuesButton.visibility = View.VISIBLE
            sellAllButton.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e("AIIntegrationFragment", "Failed to fetch investment data", e)
            if (isAdded) {
                Toast.makeText(requireContext(), "Failed to fetch investment data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchExchangeRate() {
        lifecycleScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://v6.exchangerate-api.com/v6/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(ExchangeRateApiService::class.java)
                val response = api.getExchangeRates("47a1ca6a80-0abeff07df-shwjzm", "EUR", "USD")

                // Verifica che il body non sia null e recupera il valore del tasso di cambio
                exchangeRate = response.conversion_rate
                Log.d("AIIntegrationFragment", "Exchange Rate EUR/USD: $exchangeRate")

            } catch (e: Exception) {
                Log.e("AIIntegrationFragment", "Failed to fetch exchange rate: ${e.message}")
                Toast.makeText(requireContext(), "Errore nel recupero del tasso di cambio.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchBestStocks() {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            investButton.visibility = View.GONE // Nascondi il pulsante Invest durante il caricamento
        }

        try {
            val bestStocks = getBestStocks(service, apiKey)
            val randomStocks = bestStocks.shuffled().take(5)
            Log.d("AIIntegrationFragment", "Fetched best stocks: $randomStocks")

            withContext(Dispatchers.Main) {
                adapter.submitList(randomStocks)
                investButton.visibility = View.VISIBLE // Assicurati che il bottone Invest sia visibile
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to fetch stocks: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to fetch stocks", e)
            }
        } finally {
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
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

    private suspend fun getBestStocks(apiService: FinnhubApiService, apiKey: String): List<StockSymbolWithQuote> {
        val symbols = getStockSymbols(apiService, apiKey)

        val lowRiskStocks = mutableListOf<StockSymbolWithQuote>()
        var count = 0
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(java.util.Date())

        for (symbol in symbols) {
            if (count >= 20) { // Riduci il numero di richieste
                break
            }
            val quote = getStockQuote(apiService, apiKey, symbol.symbol)

            if (quote != null) {
                quote.valdata = currentDate
            } else {
                // Se la chiamata API fallisce, interrompi il ciclo
                break
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

    private suspend fun getStockQuote(apiService: FinnhubApiService, apiKey: String, symbol: String): StockQuote? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStockQuote(symbol, apiKey).execute()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e("AIIntegrationFragment", "Failed to get stock quote for $symbol: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("AIIntegrationFragment", "Exception during API call for $symbol: ${e.message}")
                null
            }
        }
    }

    private fun calculateRisk(quote: StockQuote): Double {
        return (quote.h - quote.l) / quote.c
    }

    private suspend fun saveInvestments(stocks: List<StockSymbolWithQuote>) {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Azioni")
        val accountDocRef = db.collection(uid).document("Account")
        val batch = db.batch()

        //val numcodAzioni = fetchNumberOfStocks()

        // Ottieni il numero di azioni dal documento Account
        val cifrainAzioni = try {
            val accountSnapshot = accountDocRef.get().await()
            accountSnapshot.getLong("cifraInAzioni")?.toInt() ?: 1 // Imposta un valore predefinito se non trovato
        } catch (e: Exception) {
            Log.e("AIIntegrationFragment", "Failed to fetch number of stocks: ${e.message}")
            1 // Imposta un valore predefinito in caso di errore
        }



        for (stock in stocks) {
            val quote = stock.quote
            val symbol = stock.symbol
            val numstocks = ((cifrainAzioni*exchangeRate)/ stocks.size) / stock.quote.c
            val docRef = userDocRef.document(symbol.symbol)
            val valoreAcqEuro = quote.c * exchangeRate
            val data = hashMapOf(
                "nomeAzione" to symbol.description,
                "valorecambioAcq" to exchangeRate,
                "valoreAcq$" to quote.c,
                "dataAcq" to quote.valdata,
                "valorecambioUlt" to exchangeRate,
                "valoreUlt$" to quote.c,
                "dataUlt" to quote.valdata,
                "valorecambioReal" to exchangeRate,
                "valoreReal$" to quote.c,
                "dataReal" to quote.valdata,
                "numeroAzioni" to numstocks
            )

            Log.d("AIIntegrationFragment", "Saving stock: $symbol, valoreAcqEuro: $valoreAcqEuro, quote.c: ${quote.c}")

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

    private suspend fun fetchNumberOfStocks(): Int? {
        val uid = user?.uid ?: return null
        val accountDocRef = db.collection(uid).document("Account")
        return try {
            val accountSnapshot = accountDocRef.get().await()
            accountSnapshot.getLong("Azioni")?.toInt()
        } catch (e: Exception) {
            Log.e("AIIntegrationFragment", "Failed to fetch number of stocks: ${e.message}")
            null
        }
    }

    private suspend fun updateAllStockValues() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .client(okHttpClient)
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
                    val valoreUlt = document.getDouble("valoreUlt") ?: 0.0
                    val valoreUltEuro = valoreUlt * exchangeRate

                    batch.update(docRef, "valoreReal", valoreUltEuro)
                    batch.update(docRef, "dataReal", currentDate)

                    if (quote.c * exchangeRate > valoreUltEuro) {
                        // Mostra un Toast se il valore è maggiore
                        Toast.makeText(context, "Il valore reale è maggiore dell'ultimo valore!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Mostra un Toast se il valore non è maggiore
                        Toast.makeText(context, "Il valore reale non è maggiore dell'ultimo valore.", Toast.LENGTH_SHORT).show()
                    }

                    batch.update(docRef, "valoreUlt", quote.c * exchangeRate)
                    batch.update(docRef, "dataUlt", currentDate)
                }
            }

            batch.commit().await()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Stock values updated successfully!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to update stock values: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to update stock values", e)
            }
        }
    }




    private suspend fun registerInvestmentTransaction(stocks: List<StockSymbolWithQuote>) {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Transaction")

        val accountDocRef = db.collection(uid).document("Account")

        val cifrainAzioni = try {
            val accountSnapshot = accountDocRef.get().await()
            accountSnapshot.getLong("cifraInAzioni")?.toInt() ?: 1
        } catch (e: Exception) {
            Log.e("AIIntegrationFragment", "Failed to fetch number of stocks: ${e.message}")
            1
        }

        val currentDateTime = LocalDateTime.now()

        // Converti il nuovo oggetto LocalDateTime in un oggetto Date
        val formattedDateTime = Date.from(currentDateTime.atZone(ZoneId.systemDefault()).toInstant())

        val data = hashMapOf(
            "amount" to cifrainAzioni,
            "category" to "Stock Investment",
            "outgoing" to true,
            "date" to formattedDateTime,
            "uid" to uid
        )

        try {
            userDocRef.document(formattedDateTime.toString()).set(data).await() // Usa la data come ID del documento
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Investment transaction registered successfully!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to register transaction: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to register transaction", e)
            }
        }
    }

    private suspend fun sellAllStocks() {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Azioni")
        val transactionDocRef = db.collection(uid).document("Account").collection("Transaction")

        try {
            val snapshot = userDocRef.get().await()
            val totalValue = snapshot.documents.sumOf { document ->
                val currentAmount = document.getDouble("valoreUlt$") ?: 0.0
                val numStocks = document.getDouble("numeroAzioni") ?: 0.0
                currentAmount * numStocks / exchangeRate
            }

            val currentDateTime = LocalDateTime.now()

            // Converti il nuovo oggetto LocalDateTime in un oggetto Date
            val formattedDateTime = Date.from(currentDateTime.atZone(ZoneId.systemDefault()).toInstant())

            // Registra la transazione di vendita
            val transactionData = hashMapOf(
                "amount" to totalValue,
                "category" to "Stock Sale",
                "outgoing" to false,
                "date" to formattedDateTime,
                "uid" to uid
            )
            transactionDocRef.document(formattedDateTime.toString()).set(transactionData).await() // Usa la data come ID del documento

            // Rimuovi tutte le azioni dal database
            for (document in snapshot.documents) {
                userDocRef.document(document.id).delete().await()
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "All stocks sold successfully! Funds have been updated.", Toast.LENGTH_SHORT).show()
                showInvestmentData()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to sell all stocks: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to sell all stocks", e)
            }
        }
    }

}
