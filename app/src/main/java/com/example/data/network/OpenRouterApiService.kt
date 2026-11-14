package com.example.data.network

import com.example.data.security.InternalVault
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    @Json(name = "model") val model: String = InternalVault.aiModel(),
    @Json(name = "messages") val messages: List<OpenRouterMessage>
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    @Json(name = "message") val message: OpenRouterMessage?
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    @Json(name = "choices") val choices: List<OpenRouterChoice>? = null
)

interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String = "Clestra",
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

object OpenRouterClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val service: OpenRouterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(InternalVault.aiEndpoint())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterApiService::class.java)
    }

    val MODEL_ID: String
        get() = InternalVault.aiModel()

    fun buildClestraSystemPrompt(userName: String, userClestraId: String): String =
        InternalVault.buildPrompt(userName, userClestraId)
}
