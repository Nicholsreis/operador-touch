package com.example.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class DynamicBaseUrlInterceptor : Interceptor {
    @Volatile
    private var baseUrl: String? = null

    fun setBaseUrl(url: String) {
        var cleanUrl = url.trim()
        if (cleanUrl.isNotEmpty()) {
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "http://$cleanUrl"
            }
            this.baseUrl = cleanUrl
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val currentBaseUrl = baseUrl
        if (!currentBaseUrl.isNullOrEmpty()) {
            val newUrl = currentBaseUrl.toHttpUrlOrNull()
            if (newUrl != null) {
                val updatedUrl = request.url.newBuilder()
                    .scheme(newUrl.scheme)
                    .host(newUrl.host)
                    .port(newUrl.port)
                    .build()
                request = request.newBuilder().url(updatedUrl).build()
            }
        }
        return chain.proceed(request)
    }
}

object NetworkClient {
    val dynamicUrlInterceptor = DynamicBaseUrlInterceptor()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.1.100:3000/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
