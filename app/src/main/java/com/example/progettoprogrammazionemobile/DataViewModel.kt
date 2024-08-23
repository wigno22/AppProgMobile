// DataViewModel.kt
package com.example.progettoprogrammazionemobile

import android.graphics.Color
import android.util.Log
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

    // LiveData per notificare il fragment riguardo a eventi come successi o errori
    private val _eventMessage = MutableLiveData<String>()
    val eventMessage: LiveData<String> get() = _eventMessage

    // Funzione per aggiornare i valori delle azioni
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
                val quote = getStockQuote(service, apiKey, symbol)
                quote?.let {
                    val docRef = userDocRef.document(symbol)
                    val valoreUlt = document.getDouble("valoreUlt") ?: 0.0
                    val valoreUltEuro = valoreUlt * exchangeRate

                    batch.update(docRef, "valoreReal", valoreUltEuro)
                    batch.update(docRef, "dataReal", currentDate)

                    batch.update(docRef, "valoreUlt", quote.c * exchangeRate)
                    batch.update(docRef, "dataUlt", currentDate)
                }
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("DataViewModel", "Failed to update stock values", e)
        }
    }

    // Funzione per aggiornare i valori delle criptovalute
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
                    val valoreUlt = document.getDouble("valoreUlt€") ?: 0.0
                    val valoreUltEuro = valoreUlt * exchangeRate
                    val currentPrice = quote.price * exchangeRate

                    batch.update(docRef, "valoreReal€", currentPrice)
                    batch.update(docRef, "dataReal", currentDate)

                    batch.update(docRef, "valoreUlt€", currentPrice)
                    batch.update(docRef, "dataUlt", currentDate)
                }
            }

            batch.commit().await()
            _eventMessage.postValue("Crypto values updated successfully!")
        } catch (e: Exception) {
            _eventMessage.postValue("Failed to update crypto values: ${e.message}")
            Log.e("DataViewModel", "Failed to update crypto values", e)
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
}

