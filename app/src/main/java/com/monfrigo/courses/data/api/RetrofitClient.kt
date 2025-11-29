package com.monfrigo.courses.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Client Retrofit singleton
 */
object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var apiKey: String = ""
    private var baseUrl: String = ""

    /**
     * Configure le client avec l'URL et la clé API
     */
    fun configure(url: String, key: String) {
        baseUrl = url
        apiKey = key
        retrofit = null // Force la recréation
    }

    /**
     * Crée l'intercepteur pour ajouter la clé API
     */
    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
    }

    /**
     * Crée le client OkHttp avec logging
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(createAuthInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Obtient l'instance Retrofit
     */
    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    /**
     * Obtient le service API
     */
    fun getApiService(): ApiService {
        return getRetrofit().create(ApiService::class.java)
    }

    /**
     * Vérifie si le client est configuré
     */
    fun isConfigured(): Boolean {
        return baseUrl.isNotEmpty() && apiKey.isNotEmpty()
    }
}