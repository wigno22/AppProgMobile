// DataViewModel.kt
package com.example.progettoprogrammazionemobile

import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DataViewModel : ViewModel() {
    private val _username = MutableLiveData<String>()
    val username: LiveData<String> get() = _username

    private val _fixedEntries = MutableLiveData<String>()
    val fixedEntries: LiveData<String> get() = _fixedEntries

    private val _fixedOut = MutableLiveData<String>()
    val fixedOut: LiveData<String> get() = _fixedOut

    private val _balance = MutableLiveData<Double>()
    val balance: LiveData<Double> get() = _balance

    fun setUsername(name: String) {
        _username.value = name
    }

    fun setFixedEntries(entries: String) {
        _fixedEntries.value = entries
    }

    fun setFixedOut(out: String) {
        _fixedOut.value = out
    }

    fun setBalance(newBalance: Double) {
        _balance.value = newBalance
    }



    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    suspend fun updateAllStockValues(
        db: FirebaseFirestore,
        user: FirebaseUser?,
        okHttpClient: OkHttpClient,
        apiKey: String,
        exchangeRate: Double
    ) {
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
                // ricevo valore delle azioni ultimo
                val quote = getStockQuote(service, apiKey, symbol)
                quote?.let {
                    val docRef = userDocRef.document(symbol)
                    val valoreUlt = document.getDouble("valoreUlt$") ?: 0.0
                    val valoreUltEuro = valoreUlt / exchangeRate

                    // metto nel valoreReal il valore ultimo che avevo registrato nel db
                    batch.update(docRef, "valoreReal", valoreUltEuro)
                    batch.update(docRef, "valoreReal$", valoreUlt)
                    batch.update(docRef, "dataReal", currentDate)

                    // confronto ultime quotazioni con quelle già registrate
                    if (quote.c > valoreUlt) {
                        // Notifica il Fragment/Activity se il valore è maggiore
                        _toastMessage.postValue("Il valore reale è maggiore dell'ultimo valore!")
                    } else {
                        // Notifica il Fragment/Activity se il valore non è maggiore
                        _toastMessage.postValue("Il valore reale non è maggiore dell'ultimo valore.")
                    }

                    // registro in valoreUlt l'ultima quotazione
                    batch.update(docRef, "valoreUlt$", quote.c)
                    batch.update(docRef, "valoreUlt", quote.c / exchangeRate)
                    batch.update(docRef, "dataUlt", currentDate)
                }
            }

            batch.commit().await()
            _toastMessage.postValue("Stock values updated successfully!")
        } catch (e: Exception) {
            _toastMessage.postValue("Failed to update stock values: ${e.message}")
            Log.e("AIIntegrationFragment", "Failed to update stock values", e)
        }
    }


    // LiveData per notificare il fragment riguardo a eventi come successi o errori
    private val _eventMessage = MutableLiveData<String>()
    val eventMessage: LiveData<String> get() = _eventMessage


    suspend fun updateAllCryptoValues(
        db: FirebaseFirestore,
        user: FirebaseUser?,
        service: CoinMarketCapApiService,
        apiKey: String,
        exchangeRate: Double
    ) {
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Criptovalute")

        try {
            val snapshot = userDocRef.get().await()
            val batch = db.batch()

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            for (document in snapshot.documents) {
                val symbol = document.id
                val quote = getCryptoQuote(service, apiKey, symbol)
                quote?.let {
                    val docRef = userDocRef.document(symbol)
                    val valoreUltEuro = document.getDouble("valoreUlt€") ?: 0.0

                    val currentPrice = quote.price / exchangeRate

                    // Aggiornamento del valore nel batch
                    batch.update(docRef, "valoreReal€", valoreUltEuro)
                    batch.update(docRef, "dataReal", currentDate)

                    if (currentPrice > valoreUltEuro) {
                        // Notifica il Fragment/Activity se il valore è maggiore
                        _toastMessage.postValue("Il valore reale è maggiore dell'ultimo valore!")
                    } else {
                        // Notifica il Fragment/Activity se il valore non è maggiore
                        _toastMessage.postValue("Il valore reale non è maggiore dell'ultimo valore.")
                    }

                    // Aggiorna il valore attuale nel batch
                    batch.update(docRef, "valoreUlt€", currentPrice)
                    batch.update(docRef, "dataUlt", currentDate)
                }
            }

            batch.commit().await()
            _toastMessage.postValue("Crypto values updated successfully!")
        } catch (e: Exception) {
            _toastMessage.postValue("Failed to update crypto values: ${e.message}")
            Log.e("YourViewModel", "Failed to update crypto values", e)
        }
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

    private val _sellStocksError = MutableLiveData<String>()
    val sellStocksError: LiveData<String> get() = _sellStocksError

     suspend fun sellAllStocks(
        db: FirebaseFirestore,
        user: FirebaseUser?)
    {
        val exchangeRate = 1.09
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Azioni")
        val transactionDocRef = db.collection(uid).document("Account").collection("Transaction")

        try {
            val snapshot = userDocRef.get().await()
            val totalValue = snapshot.documents.sumOf { document ->
                val currentAmount = document.getDouble("valoreUlt") ?: 0.0
                val numStocks = document.getDouble("numeroAzioni") ?: 0.0
                (currentAmount * numStocks)
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
            transactionDocRef.document(formattedDateTime.toString()).set(transactionData)
                .await() // Usa la data come ID del documento

            // Rimuovi tutte le azioni dal database
            for (document in snapshot.documents) {
                userDocRef.document(document.id).delete().await()
            }

        } catch (e: Exception) {
            _sellStocksError.postValue("Failed to sell all stocks: ${e.message}")
            Log.e("AIIntegrationFragment", "Failed to sell all stocks", e)
        }

    }

    private val _sellCryptoError = MutableLiveData<String>()
    val sellCryptoError: LiveData<String> get() = _sellCryptoError

    suspend fun sellAllCryptos(
        db: FirebaseFirestore,
        user: FirebaseUser?)
    {
        val exchangeRate = 1.09
        val uid = user?.uid ?: return
        val userDocRef = db.collection(uid).document("Account").collection("Criptovalute")
        val transactionDocRef = db.collection(uid).document("Account").collection("Transaction")

        try {
            val snapshot = userDocRef.get().await()
            val totalValue = snapshot.documents.sumOf { document ->
                val currentAmount = document.getDouble("valoreUlt€") ?: 0.0
                val numCryptos = document.getDouble("numeroCriptovalute") ?: 0.0
                currentAmount * numCryptos
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
            transactionDocRef.document(formattedDateTime.toString()).set(transactionData).await()

            // Rimuovi tutte le criptovalute dal database
            for (document in snapshot.documents) {
                userDocRef.document(document.id).delete().await()
            }

        } catch (e: Exception) {
            _sellCryptoError.postValue("Failed to sell all stocks: ${e.message}")
            Log.e("AIIntegrationFragment", "Failed to sell all stocks", e)
        }
    }

}

