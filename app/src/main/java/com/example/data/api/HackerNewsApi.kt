package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface HackerNewsApi {
    @GET("v0/topstories.json")
    suspend fun getTopStories(): List<Int>

    @GET("v0/item/{id}.json")
    suspend fun getItem(@Path("id") id: Int): HnItem
}
