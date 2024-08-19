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
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.await
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class AiIntegrationFragment : Fragment() {

    private val apiKey = "cqrngv9r01quefaheobgcqrngv9r01quefaheoc0"
    //private val apiKeyAlpha = "IAR74U1H4BNDN1BP"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAndFundAdapter
    private lateinit var investButton: Button
    private lateinit var showStocksButton: Button
    private lateinit var updateValuesButton: Button
    private lateinit var sellAllButton: Button
    private lateinit var stockSelectionContainer: View
    private lateinit var investmentDataContainer: View
    private lateinit var investmentDataTextView: TextView
    private lateinit var progressBar: ProgressBar
    private var exchangeRate: Double = 1.09
    private var investmentType: String = "azioni"
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
        sellAllButton = view.findViewById(R.id.button_sellall)
        stockSelectionContainer = view.findViewById(R.id.stock_selection_container)
        investmentDataContainer = view.findViewById(R.id.investment_data_container)
        investmentDataTextView = view.findViewById(R.id.investment_data)
        progressBar = view.findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = StockAndFundAdapter()
        recyclerView.adapter = adapter


        // Ottieni il parametro dall'argomento
        investmentType = arguments?.getString("investmentType", "fondi") ?: "azioni"

        // Configura il testo del pulsante in base al tipo di investimento
        showStocksButton.text = if (investmentType == "azioni") "Show Stocks" else "Show Funds"


        if (investmentType == "azioni") {
            setupStockButtons()
        } else {
            setupFundButtons()
        }


        return view
    }

    private fun setupStockButtons() {
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
    }


    private fun setupFundButtons() {
        showStocksButton.setOnClickListener {
            stockSelectionContainer.visibility = View.VISIBLE
            investmentDataContainer.visibility = View.GONE
            showStocksButton.visibility = View.GONE
            updateValuesButton.visibility = View.GONE
            lifecycleScope.launch {
                try {
                    fetchBestFunds()
                } catch (e: Exception) {
                    Log.e("AIIntegrationFragment", "Error fetching funds", e)
                }
            }
        }

        investButton.setOnClickListener {
            val selectedFunds = adapter.getSelectedFunds()
            if (selectedFunds.isEmpty()) {
                Toast.makeText(requireContext(), "Please select funds to invest in", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    saveFundInvestments(selectedFunds)
                    registerFundInvestmentTransaction(selectedFunds)
                    showFundData()
                }
            }
        }

        updateValuesButton.setOnClickListener {
            lifecycleScope.launch {
                updateAllFundValues()
                showFundData()
            }
        }

        sellAllButton.setOnClickListener {
            lifecycleScope.launch {
                sellAllFunds()
            }
        }

        // Chiamata per visualizzare i fondi esistenti all'avvio del fragment
        lifecycleScope.launch {
            showFundData()
        }
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

    val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofitYahooFinance = Retrofit.Builder()
        .baseUrl("https://query1.finance.yahoo.com/")
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val yahooFinanceService = retrofitYahooFinance.create(YahooFinanceApiService::class.java)
    private val finnhubService = retrofit.create(FinnhubApiService::class.java)





    private suspend fun showFundData() {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Fondi")

        try {
            val snapshot = userDocRef.get().await()
            val funds = snapshot.documents.mapNotNull { document ->
                document.toObject(FundSymbolWithQuote::class.java)
            }

            if (funds.isEmpty()) {
                Log.d("AIIntegrationFragment", "No funds found in database.")
            } else {
                Log.d("AIIntegrationFragment", "Funds fetched: $funds")
            }

            withContext(Dispatchers.Main) {
                investmentDataContainer.visibility = View.VISIBLE
                stockSelectionContainer.visibility = View.GONE
                showStocksButton.visibility = View.VISIBLE
                updateValuesButton.visibility = View.VISIBLE
                adapter.submitList(funds)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to load fund data: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to load fund data", e)
            }
        }
    }


    private suspend fun fetchBestFunds() {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            investButton.visibility = View.GONE
        }

        try {
            val bestFunds = getBestFunds(yahooFinanceService, "NASDAQ")
            val randomFunds = bestFunds.shuffled().take(5)
            Log.d("AIIntegrationFragment", "Fetched best funds: $randomFunds")

            withContext(Dispatchers.Main) {
                adapter.submitList(randomFunds)
                investButton.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to fetch funds: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to fetch funds", e)
            }
        } finally {
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
            }
        }
    }




    private suspend fun getBestFunds(apiService: YahooFinanceApiService, query: String): List<FundSymbolWithQuote> {
        val symbols = getFundSymbols(apiService, query)

        if (symbols == null || symbols.isEmpty()) {
            Log.e("AIIntegrationFragment", "No fund symbols found.")
            return emptyList()
        }

        val lowRiskFunds = mutableListOf<FundSymbolWithQuote>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(java.util.Date())

        var count = 0
        val maxFundsToFetch = 100 // Aumenta il numero massimo di fondi da elaborare

        for (symbol in symbols) {
            if (count >= 5) { // Aumenta il numero minimo di fondi da restituire
                break
            }

            val quote = getFundQuote(apiService, symbol.symbol)

            if (quote != null) {
                quote.valdata = currentDate

                // Calcola il rischio solo se i dati sono validi
                val risk = calculateFundRisk(quote.c, quote.pc)

                if (risk < 0.1) {
                    lowRiskFunds.add(FundSymbolWithQuote(symbol, quote))
                    count++
                } else {
                    Log.d("AIIntegrationFragment", "Fund ${symbol.symbol} risk too high: $risk")
                }
            } else {
                Log.e("AIIntegrationFragment", "Failed to fetch quote for symbol: ${symbol.symbol}")
            }
        }

        if (lowRiskFunds.isEmpty()) {
            Log.d("AIIntegrationFragment", "No low-risk funds found.")
        }

        Log.d("AIIntegrationFragment", "Calculated low-risk funds: $lowRiskFunds")
        return lowRiskFunds
    }

    private suspend fun getFundSymbols(apiService: YahooFinanceApiService, query: String): List<FundSymbol>? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFundSymbols(query).execute()
                val responseBody = response.body()?.string() ?: "null"
                Log.d("AIIntegrationFragment", "Raw response: $responseBody")

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val quotesArray = json.optJSONArray("quotes")
                    val fundSymbols = mutableListOf<FundSymbol>()

                    if (quotesArray != null) {
                        for (i in 0 until quotesArray.length()) {
                            val quote = quotesArray.getJSONObject(i)
                            val symbol = quote.getString("symbol")
                            val name = quote.getString("shortname")
                            fundSymbols.add(FundSymbol(symbol, name))
                        }
                    }

                    Log.d("AIIntegrationFragment", "Fetched best funds: $fundSymbols")
                    return@withContext fundSymbols
                } else {
                    Log.e("AIIntegrationFragment", "Failed to fetch fund symbols: ${response.errorBody()?.string()}")
                }
                null
            } catch (e: Exception) {
                Log.e("AIIntegrationFragment", "Exception while fetching fund symbols: ${e.message}")
                null
            }
        }
    }

    private suspend fun getFundQuote(apiService: YahooFinanceApiService, symbol: String): FundQuote? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFundQuote(symbol).execute()
                val responseBody = response.body()?.string() ?: "null"
                Log.d("AIIntegrationFragment", "Raw response for $symbol: $responseBody")

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val chart = json.optJSONObject("chart") ?: return@withContext null
                    val resultArray = chart.optJSONArray("result") ?: return@withContext null
                    val result = resultArray.optJSONObject(0) ?: return@withContext null
                    val indicators = result.optJSONObject("indicators") ?: return@withContext null
                    val quoteArray = indicators.optJSONArray("quote")?.optJSONObject(0) ?: return@withContext null

                    // Usa valori predefiniti se alcuni dati non sono presenti
                    val openPrices = quoteArray.optJSONArray("open") ?: JSONArray().put(0.0)
                    val closePrices = quoteArray.optJSONArray("close") ?: JSONArray().put(0.0)
                    val highPrices = quoteArray.optJSONArray("high") ?: JSONArray().put(0.0)
                    val lowPrices = quoteArray.optJSONArray("low") ?: JSONArray().put(0.0)
                    val previousClose = result.optJSONObject("meta")?.optDouble("chartPreviousClose", 0.0) ?: 0.0

                    if (closePrices.length() == 0 || openPrices.length() == 0 || highPrices.length() == 0 || lowPrices.length() == 0) {
                        Log.e("AIIntegrationFragment", "Incomplete price data for symbol $symbol")
                        return@withContext null
                    }

                    val currentPrice = closePrices.optDouble(closePrices.length() - 1, 0.0)
                    val highPrice = (0 until highPrices.length()).map { highPrices.optDouble(it, 0.0) }.maxOrNull() ?: currentPrice
                    val lowPrice = (0 until lowPrices.length()).map { lowPrices.optDouble(it, 0.0) }.minOrNull() ?: currentPrice
                    val openPrice = openPrices.optDouble(openPrices.length() - 1, 0.0)

                    FundQuote(
                        symbol = symbol,
                        c = currentPrice,
                        h = highPrice,
                        l = lowPrice,
                        o = openPrice,
                        pc = previousClose,
                        valdata = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(java.util.Date())
                    )
                } else {
                    Log.e("AIIntegrationFragment", "Failed to fetch fund quote for symbol $symbol: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("AIIntegrationFragment", "Exception while fetching fund quote for symbol $symbol: ${e.message}")
                null
            }
        }
    }


    fun calculateFundRisk(currentPrice: Double, previousClose: Double): Double {
        if (currentPrice.isNaN() || previousClose.isNaN() || previousClose == 0.0) {
            return Double.NaN // Gestisci i casi in cui i dati non sono validi
        }
        return (currentPrice - previousClose) / previousClose
    }



    private suspend fun saveFundInvestments(funds: List<FundSymbolWithQuote>) {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Fondi")
        val accountDocRef = db.collection(uid).document("Account")
        val batch = db.batch()

        // Ottieni il numero di fondi dal documento Account
        val cifrainFondi = try {
            val accountSnapshot = accountDocRef.get().await()
            accountSnapshot.getLong("cifraInFondi")?.toInt() ?: 1 // Imposta un valore predefinito se non trovato
        } catch (e: Exception) {
            Log.e("AIIntegrationFragment", "Failed to fetch number of funds: ${e.message}")
            1 // Imposta un valore predefinito in caso di errore
        }

        for (fund in funds) {
            val quote = fund.quote
            val symbol = fund.symbol
            val numFunds = ((cifrainFondi * exchangeRate) / funds.size) / fund.quote.c
            val docRef = userDocRef.document(symbol.symbol)
            val valoreAcqEuro = quote.c * exchangeRate
            val data = hashMapOf(
                "nomeFondo" to symbol.description,
                "valorecambioAcq" to exchangeRate,
                "valoreAcq$" to quote.c,
                "dataAcq" to quote.valdata,
                "valorecambioUlt" to exchangeRate,
                "valoreUlt$" to quote.c,
                "dataUlt" to quote.valdata,
                "valorecambioReal" to exchangeRate,
                "valoreReal$" to quote.c,
                "dataReal" to quote.valdata,
                "numeroFondi" to numFunds
            )

            Log.d("AIIntegrationFragment", "Saving fund: $symbol, valoreAcqEuro: $valoreAcqEuro, quote.c: ${quote.c}")

            batch.set(docRef, data)
        }

        try {
            batch.commit().await()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Fund investments saved successfully!", Toast.LENGTH_SHORT).show()
                investButton.visibility = View.GONE // Nascondi il bottone Invest dopo aver confermato
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to save fund investments: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to save fund investments", e)
            }
        }
    }

    private suspend fun registerFundInvestmentTransaction(funds: List<FundSymbolWithQuote>) {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Transaction")

        val accountDocRef = db.collection(uid).document("Account")

        val cifrainFondi = try {
            val accountSnapshot = accountDocRef.get().await()
            accountSnapshot.getLong("cifraInFondi")?.toInt() ?: 1
        } catch (e: Exception) {
            Log.e("AIIntegrationFragment", "Failed to fetch number of funds: ${e.message}")
            1
        }

        val currentDateTime = LocalDateTime.now()

        // Converti il nuovo oggetto LocalDateTime in un oggetto Date
        val formattedDateTime = Date.from(currentDateTime.atZone(ZoneId.systemDefault()).toInstant())

        val data = hashMapOf(
            "amount" to cifrainFondi,
            "category" to "Fund Investment",
            "outgoing" to true,
            "date" to formattedDateTime,
            "uid" to uid
        )

        try {
            userDocRef.document(formattedDateTime.toString()).set(data).await() // Usa la data come ID del documento
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Fund investment transaction registered successfully!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to register transaction: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to register transaction", e)
            }
        }
    }

    private suspend fun updateAllFundValues() {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Fondi")

        try {
            val snapshot = userDocRef.get().await()
            val batch = db.batch()

            for (document in snapshot.documents) {
                val fund = document.toObject(FundSymbolWithQuote::class.java)
                if (fund != null) {
                    val updatedQuote = getFundQuote(yahooFinanceService, fund.symbol.symbol)
                    if (updatedQuote != null) {
                        fund.quote = updatedQuote
                        batch.set(document.reference, fund)
                    }
                }
            }

            batch.commit().await()

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "All fund values updated successfully!", Toast.LENGTH_SHORT).show()
                showFundData()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to update fund values: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to update fund values", e)
            }
        }
    }

    private suspend fun sellAllFunds() {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Fondi")

        try {
            val snapshot = userDocRef.get().await()
            val batch = db.batch()

            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }

            batch.commit().await()

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "All funds sold successfully!", Toast.LENGTH_SHORT).show()
                showFundData()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to sell all funds: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to sell all funds", e)
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
                val numStocks = data?.get("numeroAzioni") as? Int

                val numberFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)

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

    private fun calculateRisk(quote: StockQuote): Double {
        return (quote.h - quote.l) / quote.c
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

    private suspend fun fetchBestStocks() {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            investButton.visibility = View.GONE // Nascondi il pulsante Invest durante il caricamento
        }

        try {
            val bestStocks = getBestStocks(finnhubService, apiKey)
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



}
