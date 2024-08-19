package com.example.progettoprogrammazionemobile

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
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
    val o: Double,  // Prezzo di apertura
    val pc: Double, // Prezzo di chiusura precedente
    var valdata: String // Data del prezzo
)



interface YahooFinanceApiService {
    @GET("v1/finance/search")
    fun getFundSymbols(
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 5,
        @Query("region") region: String = "US"
    ): Call<ResponseBody>

    @GET("v8/finance/chart/{symbol}")
    fun getFundQuote(
        @Path("symbol") symbol: String,
        @Query("region") region: String = "US",
        @Query("interval") interval: String = "1d"
    ): Call<ResponseBody>
}




data class FundSymbolWithQuote(
    val symbol: FundSymbol,
    var quote: FundQuote
) {
    // Questo costruttore senza argomenti è necessario per la deserializzazione di Firebase Firestore
}

data class FundSymbolsResponse(
    @SerializedName("data")
    val data: List<FundSymbol>?
)

data class FundSymbol(
    val symbol: String = "",
    val name: String = "",
    val description: String = ""
)


data class FundQuote(
    val symbol: String = "",
    val c: Double = 0.0,
    val h: Double = 0.0,
    val l: Double = 0.0,
    val o: Double = 0.0,
    val pc: Double = 0.0,
    var valdata: String = ""
)
