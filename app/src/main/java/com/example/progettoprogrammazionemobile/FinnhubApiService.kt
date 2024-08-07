package com.example.progettoprogrammazionemobile

import retrofit2.Call
import retrofit2.http.GET
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
}
