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
import kotlin.math.absoluteValue

class FundFragment : Fragment() {

    private val apiKey = "6e362c44-bcd8-483d-9db5-a3f880533d6f"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CryptoAdapter
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
        val view = inflater.inflate(R.layout.fragment_fund, container, false)
        recyclerView = view.findViewById(R.id.recycler_view_crypto)
        investButton = view.findViewById(R.id.button_invest_crypto)
        showStocksButton = view.findViewById(R.id.button_show_crypto)
        updateValuesButton = view.findViewById(R.id.button_update_values_crypto)
        sellAllButton = view.findViewById(R.id.button_sellallcrypto)
        stockSelectionContainer = view.findViewById(R.id.crypto_selection_container)
        investmentDataContainer = view.findViewById(R.id.investment_data_container_crypto)
        investmentDataTextView = view.findViewById(R.id.investment_data_crypto)
        progressBar = view.findViewById(R.id.progressBarCrypto)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CryptoAdapter()
        recyclerView.adapter = adapter

        showStocksButton.setOnClickListener {
            stockSelectionContainer.visibility = View.VISIBLE
            investmentDataContainer.visibility = View.GONE
            showStocksButton.visibility = View.GONE
            updateValuesButton.visibility = View.GONE
            lifecycleScope.launch {
                try {
                    fetchBestCrypto()
                } catch (e: Exception) {
                    Log.e("AIIntegrationFragment", "Error fetching stocks", e)
                }
            }
        }

        investButton.setOnClickListener {
            val selectedCrypto = adapter.getSelectedCrypto()
            if (selectedCrypto.isEmpty()) {
                Toast.makeText(requireContext(), "Please select Crypto to invest in", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    // Salva gli investimenti selezionati (logica non implementata)
                    saveInvestments(selectedCrypto)
                    registerInvestmentTransaction(selectedCrypto)
                    showInvestmentData()
                }
            }
        }

        updateValuesButton.setOnClickListener {
            lifecycleScope.launch {
                // Aggiorna i valori delle azioni selezionate (logica non implementata)
                updateAllCryptoValues()
                showInvestmentData()
            }
        }

        sellAllButton.setOnClickListener {
            lifecycleScope.launch {
                // Vendi tutte le criptovalute (logica non implementata)
                sellAllCryptos()
            }
        }

        // Chiamata per visualizzare le azioni esistenti all'avvio del fragment
        lifecycleScope.launch {
            // Mostra i dati degli investimenti esistenti (logica non implementata)
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
        .baseUrl("https://pro-api.coinmarketcap.com/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(CoinMarketCapApiService::class.java)



    private suspend fun showInvestmentData() {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Criptovalute")

        try {
            val snapshot = userDocRef.get().await()

            if (snapshot.isEmpty) {
                // Se non ci sono criptovalute, mostra il pulsante per visualizzare le migliori criptovalute
                showStocksButton.visibility = View.VISIBLE // Nascondi il pulsante Visualizza Criptovalute se non ci sono criptovalute
                return
            }

            val investmentData = StringBuilder()
            for (document in snapshot.documents) {
                val data = document.data

                val cryptoName = data?.get("nomeCripto") as? String
                val purchaseAmount = data?.get("valoreAcq€") as? Double
                val realAmount = data?.get("valoreReal€") as? Double
                val currentAmount = data?.get("valoreUlt€") as? Double
                val dateUlt = data?.get("dataUlt") as? String
                val numCryptos = data?.get("numeroCriptovalute") as? Int

                val numberFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)

                if (cryptoName != null) {
                    investmentData.append("Name: $cryptoName\n")
                    investmentData.append("Price\n")
                    investmentData.append("Purchase: ${purchaseAmount?.let { numberFormat.format(it) } ?: "N/A"}\n")
                    investmentData.append("Last: ${realAmount?.let { numberFormat.format(it) } ?: "N/A"}\n")
                    investmentData.append("Current: ${currentAmount?.let { numberFormat.format(it) } ?: "N/A"}\n")
                    investmentData.append("Current Date: ${dateUlt ?: "N/A"}\n")
                    investmentData.append("Number of Cryptos: ${numCryptos ?: "N/A"}\n")
                    investmentData.append("\n")
                }
            }

            investmentDataTextView.text = investmentData.toString()
            stockSelectionContainer.visibility = View.GONE
            investmentDataContainer.visibility = View.VISIBLE
            showStocksButton.visibility = View.GONE // Nascondi il pulsante Visualizza Criptovalute se ci sono già criptovalute
            updateValuesButton.visibility = View.VISIBLE
            sellAllButton.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e("AIIntegrationFragment", "Failed to fetch investment data", e)
            if (isAdded) {
                Toast.makeText(requireContext(), "Failed to fetch investment data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }



    private suspend fun fetchBestCrypto() {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            investButton.visibility = View.GONE // Nascondi il pulsante Invest durante il caricamento
        }

        try {
            val bestCrypto = getBestCrypto(service, apiKey)
            val randomCrypto = bestCrypto.shuffled().take(5)
            Log.d("AIIntegrationFragment", "Fetched best cryptos: $randomCrypto")


            withContext(Dispatchers.Main) {
                adapter.submitList(randomCrypto)
                investButton.visibility = View.VISIBLE // Assicurati che il bottone Invest sia visibile
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to fetch cryptos: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to fetch cryptos", e)
            }
        } finally {
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                investButton.visibility = View.VISIBLE // Assicurati che il bottone Invest sia visibile
            }
        }
    }

    private suspend fun getBestCrypto(apiService: CoinMarketCapApiService, apiKey: String): List<CryptoSymbolWithQuote> {
        val symbols = getCryptoSymbols(apiService, apiKey)

        val lowRiskCryptos = mutableListOf<CryptoSymbolWithQuote>()
        var count = 0

        for (symbol in symbols) {
            if (count >= 20) { // Limita il numero di richieste
                break
            }
            val quote = getCryptoQuote(apiService, apiKey, symbol.symbol)

            quote?.let {
                val risk = calculateCryptoRisk(it)
                if (risk < 0.1) {
                    lowRiskCryptos.add(CryptoSymbolWithQuote(symbol, it))
                    count++
                }
            }
        }

        Log.d("AIIntegrationFragment", "Calculated low risk cryptos: $lowRiskCryptos")
        return lowRiskCryptos
    }

    private fun calculateCryptoRisk(quote: CryptoQuote): Double {
        return quote.percent_change_24h.absoluteValue / 100.0
    }

    private suspend fun getCryptoSymbols(apiService: CoinMarketCapApiService, apiKey: String): List<CryptoSymbol> {
        return withContext(Dispatchers.IO) {
            val response = apiService.getCryptoSymbols(apiKey = apiKey).execute()
            if (response.isSuccessful) {
                response.body()?.data ?: emptyList()
            } else {
                Log.e("AIIntegrationFragment", "Failed to get crypto symbols: ${response.errorBody()?.string()}")
                emptyList()
            }
        }
    }

    private suspend fun getCryptoQuote(apiService: CoinMarketCapApiService, apiKey: String, symbol: String): CryptoQuote? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCryptoQuote(symbol, apiKey).execute()
                if (response.isSuccessful) {
                    response.body()?.data?.get(symbol)?.quote?.get("USD")
                } else {
                    Log.e("AIIntegrationFragment", "Failed to get crypto quote for $symbol: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("AIIntegrationFragment", "Exception during API call for $symbol: ${e.message}")
                null
            }
        }
    }

    private suspend fun saveInvestments(cryptos: List<CryptoSymbolWithQuote>) {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Fondi")
        val accountDocRef = db.collection(uid).document("Account")
        val batch = db.batch()

        // Ottieni il numero di criptovalute dal documento Account
        val cifrainCriptovalute = try {
            val accountSnapshot = accountDocRef.get().await()
            accountSnapshot.getLong("cifraInFondi")?.toInt() ?: 1 // Imposta un valore predefinito se non trovato
        } catch (e: Exception) {
            Log.e("AIIntegrationFragment", "Failed to fetch number of cryptos: ${e.message}")
            1 // Imposta un valore predefinito in caso di errore
        }

        for (crypto in cryptos) {
            val quote = crypto.quote
            val symbol = crypto.symbol
            val numCryptos = ((cifrainCriptovalute * exchangeRate) / cryptos.size) / crypto.quote.price
            val docRef = userDocRef.document(symbol.symbol)
            val valoreAcqEuro = quote.price * exchangeRate
            val data = hashMapOf(
                "nomeCripto" to symbol.name,
                "valorecambioAcq" to exchangeRate,
                "valoreAcq€" to valoreAcqEuro,
                //"dataAcq" to quote.valdata,
                "valorecambioUlt" to exchangeRate,
                "valoreUlt€" to quote.price * exchangeRate,
                //"dataUlt" to quote.valdata,
                "valorecambioReal" to exchangeRate,
                "valoreReal€" to quote.price * exchangeRate,
                //"dataReal" to quote.valdata,
                "numeroCriptovalute" to numCryptos
            )

            Log.d("AIIntegrationFragment", "Saving crypto: $symbol, valoreAcqEuro: $valoreAcqEuro, quote.price: ${quote.price}")

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


    private suspend fun updateAllCryptoValues() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CoinMarketCapApiService::class.java)
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Criptovalute")

        try {
            val snapshot = userDocRef.get().await()
            val batch = db.batch()

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(java.util.Date())

            for (document in snapshot.documents) {
                val symbol = document.id
                val quote = getCryptoQuote(service, apiKey, symbol)
                quote?.let {
                    val docRef = userDocRef.document(symbol)
                    val valoreUlt = document.getDouble("valoreUlt€") ?: 0.0
                    val valoreUltEuro = valoreUlt * exchangeRate

                    batch.update(docRef, "valoreReal€", quote.price * exchangeRate)
                    batch.update(docRef, "dataReal", currentDate)

                    if (quote.price * exchangeRate > valoreUltEuro) {
                        // Mostra un Toast se il valore è maggiore
                        Toast.makeText(context, "Il valore reale è maggiore dell'ultimo valore!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Mostra un Toast se il valore non è maggiore
                        Toast.makeText(context, "Il valore reale non è maggiore dell'ultimo valore.", Toast.LENGTH_SHORT).show()
                    }

                    batch.update(docRef, "valoreUlt€", quote.price * exchangeRate)
                    batch.update(docRef, "dataUlt", currentDate)
                }
            }

            batch.commit().await()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Crypto values updated successfully!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to update crypto values: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to update crypto values", e)
            }
        }
    }
    private suspend fun registerInvestmentTransaction(cryptos: List<CryptoSymbolWithQuote>) {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Transaction")

        val accountDocRef = db.collection(uid).document("Account")

        val cifrainCriptovalute = try {
            val accountSnapshot = accountDocRef.get().await()
            accountSnapshot.getLong("cifraInFondi")?.toInt() ?: 1
        } catch (e: Exception) {
            Log.e("AIIntegrationFragment", "Failed to fetch number of cryptos: ${e.message}")
            1
        }

        val currentDateTime = LocalDateTime.now()
        val formattedDateTime = Date.from(currentDateTime.atZone(ZoneId.systemDefault()).toInstant())

        val data = hashMapOf(
            "amount" to cifrainCriptovalute,
            "category" to "Crypto Investment",
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


    private suspend fun sellAllCryptos() {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Criptovalute")
        val transactionDocRef = db.collection(uid).document("Account").collection("Transaction")

        try {
            val snapshot = userDocRef.get().await()
            val totalValue = snapshot.documents.sumOf { document ->
                val currentAmount = document.getDouble("valoreUlt€") ?: 0.0
                val numCryptos = document.getDouble("numeroCriptovalute") ?: 0.0
                currentAmount * numCryptos / exchangeRate
            }

            val currentDateTime = LocalDateTime.now()
            val formattedDateTime = Date.from(currentDateTime.atZone(ZoneId.systemDefault()).toInstant())

            // Registra la transazione di vendita
            val transactionData = hashMapOf(
                "amount" to totalValue,
                "category" to "Crypto Sale",
                "outgoing" to false,
                "date" to formattedDateTime,
                "uid" to uid
            )
            transactionDocRef.document(formattedDateTime.toString()).set(transactionData).await() // Usa la data come ID del documento

            // Rimuovi tutte le criptovalute dal database
            for (document in snapshot.documents) {
                userDocRef.document(document.id).delete().await()
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "All cryptos sold successfully! Funds have been updated.", Toast.LENGTH_SHORT).show()
                showInvestmentData()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to sell all cryptos: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AIIntegrationFragment", "Failed to sell all cryptos", e)
            }
        }
    }





}
