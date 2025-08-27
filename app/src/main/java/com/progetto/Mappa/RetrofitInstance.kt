package com.progetto.Mappa

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/") // Base URL Google Maps
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val directionsApi: DirectionsApiService by lazy {
        retrofit.create(DirectionsApiService::class.java)
    }
}
