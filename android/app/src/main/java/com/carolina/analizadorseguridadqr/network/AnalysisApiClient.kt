package com.carolina.analizadorseguridadqr.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AnalysisApiClient {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // Exponemos la instancia compartida para reutilizar la misma configuracion
    // de red en APIs futuras sin duplicar baseUrl, cliente o Gson.
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BackendConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: AnalysisApi by lazy {
        retrofit.create(AnalysisApi::class.java)
    }
}
