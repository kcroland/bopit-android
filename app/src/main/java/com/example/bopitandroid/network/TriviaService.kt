package com.example.bopitandroid.network

import retrofit2.Call
import retrofit2.http.GET

private const val ENDPOINT = "api.php?amount=1&category=9&difficulty=easy&type=boolean"

interface TriviaService {
    @GET(ENDPOINT)
    fun getQuestions() : Call<MyTrivia>
}