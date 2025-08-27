package com.progetto.Mappa

import com.progetto.Mappa.model.DirectionsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsApiService {
    @GET("maps/api/directions/json") // Endpoint relativo all'URL base di Retrofit
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "walking", // Valore di default per la modalità
        @Query("key") apiKey: String
    ): Response<DirectionsResponse> // Response<T> di Retrofit per gestire i dettagli della risposta HTTP
}