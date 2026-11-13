package com.example.data.network

import com.example.data.model.PendingMessageItem
import com.example.data.model.RelayHealthResponse
import com.example.data.model.SendMessagePayload
import com.example.data.model.SendMessageResponse
import com.example.data.model.SyncResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface RelayApiService {

    @POST("api/messages/send")
    suspend fun sendOfflineMessage(
        @Body payload: SendMessagePayload
    ): SendMessageResponse

    @GET("api/messages/sync")
    suspend fun syncOfflineMessages(
        @Query("recipientId") recipientId: String
    ): SyncResponse

    @GET("api/health")
    suspend fun getHealth(): RelayHealthResponse
}

object NetworkClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun createRelayService(baseUrl: String): RelayApiService {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RelayApiService::class.java)
    }
}
