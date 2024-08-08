package com.example.progettoprogrammazionemobile

import com.google.android.gms.common.api.Response
import retrofit2.Call
import retrofit2.http.GET
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