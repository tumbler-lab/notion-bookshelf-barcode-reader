package com.example.notionbookshelfbarcodereader

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface IssNdlService {
    @GET("api/sru")
    fun fetchBookInfo(
        @Query("operation") operation: String,
        @Query("query") query: String,
        @Query("recordPacking") recordPacking: String,
    ): Call<ResponseBody>
}