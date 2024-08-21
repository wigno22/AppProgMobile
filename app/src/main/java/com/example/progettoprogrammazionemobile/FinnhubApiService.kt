package com.example.progettoprogrammazionemobile

import com.google.android.gms.common.api.Response
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface FinnhubApiService {
    @GET("stock/symbol")
    fun getStockSymbols(
        @Query("exchange") exchange: String,
        @Query("token") apiKey: String
    ): Call<List<StockSymbol>>

    @GET("quote")
    fun getStockQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): Call<StockQuote>

    @GET("forex/rates")
    suspend fun getExchangeRate(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): ExchangeRateResponse


}



interface ExchangeRateApiService {
    @GET("{apiKey}/latest")
    suspend fun getExchangeRates(
        @Path("apiKey") apiKey: String,
        @Query("base") baseCurrency: String,
        @Query("symbols") targetCurrency: String
    ): ExchangeRateResponseE
}

data class ExchangeRateResponseE(
    val conversion_rate: Double
)

// Finnhub Response Model
data class ExchangeRateResponse(
    val quote: Map<String, Double>
)

data class StockSymbolWithQuote(
    val symbol: StockSymbol,
    val quote: StockQuote
)

data class StockSymbol(
    val symbol: String,
    val description: String

)

data class StockQuote(
    val c: Double,  // Prezzo corrente
    val h: Double,  // Prezzo massimo del giorno
    val l: Double,  // Prezzo minimo del giorno
    var valdata: String
)

interface CoinMarketCapApiService {
    @GET("cryptocurrency/listings/latest")
    fun getCryptoSymbols(
        @Query("CMC_PRO_API_KEY") apiKey: String
    ): Call<CryptoListingsResponse>

    @GET("cryptocurrency/quotes/latest")
    fun getCryptoQuote(
        @Query("symbol") symbol: String,
        @Query("CMC_PRO_API_KEY") apiKey: String
    ): Call<CryptoQuoteResponse>
}

data class CryptoListingsResponse(
    val data: List<CryptoSymbol>
)

data class CryptoQuoteResponse(
    val data: Map<String, CryptoQuoteDetails>
)

data class CryptoQuoteDetails(
    val quote: Map<String, CryptoQuote>
)

data class CryptoSymbol(
    val symbol: String,
    val name: String,
    val slug: String,
    val cmc_rank: Int
)

data class CryptoQuote(
    val price: Double,
    val volume_24h: Double,
    val percent_change_1h: Double,
    val percent_change_24h: Double,
    val percent_change_7d: Double
)


data class CryptoSymbolWithQuote(
    val symbol: CryptoSymbol,
    val quote: CryptoQuote
)
