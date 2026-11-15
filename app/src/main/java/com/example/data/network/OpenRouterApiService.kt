package com.example.data.network

import com.example.data.security.InternalVault
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenRouterImageUrl(
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterContentPart(
    @Json(name = "type") val type: String,
    @Json(name = "text") val text: String? = null,
    @Json(name = "image_url") val imageUrl: OpenRouterImageUrl? = null
) {
    companion object {
        fun text(value: String) = OpenRouterContentPart(type = "text", text = value)
        fun imageUrl(dataOrHttpUrl: String) = OpenRouterContentPart(
            type = "image_url",
            imageUrl = OpenRouterImageUrl(url = dataOrHttpUrl)
        )
    }
}

sealed class OpenRouterContent {
    data class Text(val text: String) : OpenRouterContent()
    data class Parts(val parts: List<OpenRouterContentPart>) : OpenRouterContent()

    fun asPlainText(): String = when (this) {
        is Text -> text
        is Parts -> parts.mapNotNull { it.text }.joinToString("\n")
    }
}

class OpenRouterContentAdapter {
    @ToJson
    fun toJson(
        writer: JsonWriter,
        value: OpenRouterContent?,
        partAdapter: JsonAdapter<OpenRouterContentPart>
    ) {
        when (value) {
            null -> writer.nullValue()
            is OpenRouterContent.Text -> writer.value(value.text)
            is OpenRouterContent.Parts -> {
                writer.beginArray()
                value.parts.forEach { part -> partAdapter.toJson(writer, part) }
                writer.endArray()
            }
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): OpenRouterContent? {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> OpenRouterContent.Text(reader.nextString())
            JsonReader.Token.NULL -> reader.nextNull<OpenRouterContent>()
            JsonReader.Token.BEGIN_ARRAY -> {
                val sb = StringBuilder()
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val name = reader.nextName()
                            if (name == "text" && reader.peek() == JsonReader.Token.STRING) {
                                if (sb.isNotEmpty()) sb.append('\n')
                                sb.append(reader.nextString())
                            } else {
                                reader.skipValue()
                            }
                        }
                        reader.endObject()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endArray()
                OpenRouterContent.Text(sb.toString())
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: OpenRouterContent? = null
) {
    constructor(role: String, text: String) : this(role = role, content = OpenRouterContent.Text(text))

    fun textContent(): String? = content?.asPlainText()?.trim()?.takeIf { it.isNotBlank() }
}

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

@JsonClass(generateAdapter = true)
data class OpenRouterModelArchitecture(
    @Json(name = "input_modalities") val inputModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelPricing(
    @Json(name = "prompt") val prompt: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelInfo(
    @Json(name = "id") val id: String,
    @Json(name = "architecture") val architecture: OpenRouterModelArchitecture? = null,
    @Json(name = "pricing") val pricing: OpenRouterModelPricing? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterModelsResponse(
    @Json(name = "data") val data: List<OpenRouterModelInfo>? = null
)

interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String = "Clestra",
        @Body request: OpenRouterRequest
    ): OpenRouterResponse

    @GET("api/v1/models")
    suspend fun listModels(): OpenRouterModelsResponse
}

object OpenRouterClient {
    val moshi: Moshi = Moshi.Builder()
        .add(OpenRouterContentAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
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

    /**
     * Picks active free vision-capable model IDs from OpenRouter's /api/v1/models response.
     */
    fun selectFreeVisionModels(models: List<OpenRouterModelInfo>): List<String> {
        val candidates = models.filter { info ->
            val supportsImage = info.architecture?.inputModalities?.any {
                it.equals("image", ignoreCase = true)
            } == true
            val isFree = info.id.endsWith(":free", ignoreCase = true) ||
                info.pricing?.prompt == "0" ||
                info.pricing?.prompt == "0.0"
            supportsImage && isFree && info.id != MODEL_ID
        }
        val priorityHints = listOf("gemini", "gemma", "qwen", "nemotron", "llama", "mistral", "pixtral")
        return candidates
            .sortedBy { info ->
                val lower = info.id.lowercase()
                val idx = priorityHints.indexOfFirst { lower.contains(it) }
                if (idx >= 0) idx else priorityHints.size
            }
            .map { it.id }
            .distinct()
    }
}
