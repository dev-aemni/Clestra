package com.example.data.network

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

enum class GifProvider(val displayName: String, val badge: String) {
    ALL("All", "ALL"),
    TENOR("Tenor", "TENOR"),
    GIPHY("GIPHY", "GIPHY"),
    KLIPY("KLIPY", "KLIPY"),
    REACTIONS("Reactions", "REACT")
}

data class GifItem(
    val id: String,
    val title: String,
    val previewUrl: String,
    val fullUrl: String,
    val provider: GifProvider
)

data class GifSearchResult(
    val items: List<GifItem>,
    val notice: String? = null
)

// --- GIPHY DTOs ---
@JsonClass(generateAdapter = true)
data class GiphyImageVariant(
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GiphyImages(
    @Json(name = "fixed_width") val fixedWidth: GiphyImageVariant? = null,
    @Json(name = "original") val original: GiphyImageVariant? = null
)

@JsonClass(generateAdapter = true)
data class GiphyGifObject(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "images") val images: GiphyImages? = null
)

@JsonClass(generateAdapter = true)
data class GiphyResponse(
    @Json(name = "data") val data: List<GiphyGifObject> = emptyList()
)

// --- Tenor & KLIPY v2 DTOs ---
@JsonClass(generateAdapter = true)
data class TenorMediaFormat(
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class TenorMediaFormats(
    @Json(name = "tinygif") val tinygif: TenorMediaFormat? = null,
    @Json(name = "gif") val gif: TenorMediaFormat? = null,
    @Json(name = "mediumgif") val mediumgif: TenorMediaFormat? = null
)

@JsonClass(generateAdapter = true)
data class TenorResultObject(
    @Json(name = "id") val id: String,
    @Json(name = "content_description") val contentDescription: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "media_formats") val mediaFormats: TenorMediaFormats? = null
)

@JsonClass(generateAdapter = true)
data class TenorResponse(
    @Json(name = "results") val results: List<TenorResultObject> = emptyList()
)

// --- Nekos.best Public Reaction GIF DTOs ---
@JsonClass(generateAdapter = true)
data class NekosBestItem(
    @Json(name = "url") val url: String? = null,
    @Json(name = "anime_name") val animeName: String? = null
)

@JsonClass(generateAdapter = true)
data class NekosBestResponse(
    @Json(name = "results") val results: List<NekosBestItem> = emptyList()
)

interface GiphyApiService {
    @GET("v1/gifs/trending")
    suspend fun trending(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 20,
        @Query("rating") rating: String = "pg-13"
    ): GiphyResponse

    @GET("v1/gifs/search")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("rating") rating: String = "pg-13"
    ): GiphyResponse
}

interface TenorApiService {
    @GET("v2/featured")
    suspend fun featured(
        @Query("key") apiKey: String,
        @Query("limit") limit: Int = 20,
        @Query("media_filter") mediaFilter: String = "gif,tinygif"
    ): TenorResponse

    @GET("v2/search")
    suspend fun search(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("media_filter") mediaFilter: String = "gif,tinygif"
    ): TenorResponse
}

interface KlipyApiService {
    @GET("v2/featured")
    suspend fun featured(
        @Query("key") apiKey: String,
        @Query("limit") limit: Int = 20
    ): TenorResponse

    @GET("v2/search")
    suspend fun search(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): TenorResponse
}

interface NekosBestApiService {
    @GET("api/v2/{category}")
    suspend fun fetchReactionGifs(
        @Path("category") category: String,
        @Query("amount") amount: Int = 10
    ): NekosBestResponse
}

object GifProviderManager {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val giphyService: GiphyApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.giphy.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GiphyApiService::class.java)
    }

    private val tenorService: TenorApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://tenor.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TenorApiService::class.java)
    }

    private val klipyService: KlipyApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.klipy.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(KlipyApiService::class.java)
    }

    private val nekosService: NekosBestApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://nekos.best/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NekosBestApiService::class.java)
    }

    private val giphyKey: String
        get() = runCatching { BuildConfig.GIPHY_API_KEY }.getOrDefault("")

    private val tenorKey: String
        get() = runCatching { BuildConfig.TENOR_API_KEY }.getOrDefault("")

    private val klipyKey: String
        get() = runCatching { BuildConfig.KLIPY_API_KEY }.getOrDefault("")

    fun isProviderConfigured(provider: GifProvider): Boolean = when (provider) {
        GifProvider.ALL -> true
        GifProvider.REACTIONS -> true
        GifProvider.GIPHY -> giphyKey.isNotBlank() && giphyKey != "MY_GIPHY_API_KEY"
        GifProvider.TENOR -> tenorKey.isNotBlank() && tenorKey != "MY_TENOR_API_KEY"
        GifProvider.KLIPY -> klipyKey.isNotBlank() && klipyKey != "MY_KLIPY_API_KEY"
    }

    suspend fun searchOrTrending(
        provider: GifProvider,
        query: String
    ): GifSearchResult = coroutineScope {
        val cleanQuery = query.trim()
        when (provider) {
            GifProvider.TENOR -> {
                if (!isProviderConfigured(GifProvider.TENOR)) {
                    GifSearchResult(
                        items = emptyList(),
                        notice = "Set TENOR_API_KEY in AI Studio Secrets to search Tenor GIFs."
                    )
                } else {
                    val items = fetchTenor(cleanQuery)
                    GifSearchResult(
                        items = items,
                        notice = if (items.isEmpty()) "No Tenor GIFs found for \"$cleanQuery\"." else null
                    )
                }
            }
            GifProvider.GIPHY -> {
                if (!isProviderConfigured(GifProvider.GIPHY)) {
                    GifSearchResult(
                        items = emptyList(),
                        notice = "Set GIPHY_API_KEY in AI Studio Secrets to search GIPHY GIFs."
                    )
                } else {
                    val items = fetchGiphy(cleanQuery)
                    GifSearchResult(
                        items = items,
                        notice = if (items.isEmpty()) "No GIPHY GIFs found for \"$cleanQuery\"." else null
                    )
                }
            }
            GifProvider.KLIPY -> {
                if (!isProviderConfigured(GifProvider.KLIPY)) {
                    GifSearchResult(
                        items = emptyList(),
                        notice = "Set KLIPY_API_KEY in AI Studio Secrets to search KLIPY GIFs."
                    )
                } else {
                    val items = fetchKlipy(cleanQuery)
                    GifSearchResult(
                        items = items,
                        notice = if (items.isEmpty()) "No KLIPY GIFs found for \"$cleanQuery\"." else null
                    )
                }
            }
            GifProvider.REACTIONS -> {
                val items = fetchReactions(cleanQuery)
                GifSearchResult(
                    items = items,
                    notice = if (items.isEmpty()) "Could not load reaction GIFs. Check internet connection." else null
                )
            }
            GifProvider.ALL -> {
                val tenorJob = async { if (isProviderConfigured(GifProvider.TENOR)) fetchTenor(cleanQuery) else emptyList() }
                val giphyJob = async { if (isProviderConfigured(GifProvider.GIPHY)) fetchGiphy(cleanQuery) else emptyList() }
                val klipyJob = async { if (isProviderConfigured(GifProvider.KLIPY)) fetchKlipy(cleanQuery) else emptyList() }
                val reactionsJob = async { fetchReactions(cleanQuery) }

                val combined = buildList {
                    val lists = listOf(tenorJob.await(), giphyJob.await(), klipyJob.await(), reactionsJob.await())
                    val maxLen = lists.maxOfOrNull { it.size } ?: 0
                    for (i in 0 until maxLen) {
                        for (list in lists) {
                            if (i < list.size) add(list[i])
                        }
                    }
                }

                val missingKeys = listOf(
                    GifProvider.TENOR,
                    GifProvider.GIPHY,
                    GifProvider.KLIPY
                ).filter { !isProviderConfigured(it) }

                val notice = when {
                    combined.isEmpty() -> "No GIFs loaded. Check network connection or add GIF API keys in Secrets."
                    missingKeys.isNotEmpty() -> "Showing live Reactions. Add Tenor, GIPHY, or KLIPY keys in Secrets to unlock all providers."
                    else -> null
                }
                GifSearchResult(items = combined, notice = notice)
            }
        }
    }

    private suspend fun fetchGiphy(query: String): List<GifItem> = try {
        val response = if (query.isBlank()) {
            giphyService.trending(apiKey = giphyKey)
        } else {
            giphyService.search(apiKey = giphyKey, query = query)
        }
        response.data.mapNotNull { item ->
            val preview = item.images?.fixedWidth?.url ?: item.images?.original?.url ?: return@mapNotNull null
            val full = item.images?.original?.url ?: preview
            GifItem(
                id = "giphy_${item.id}",
                title = item.title?.ifBlank { "GIPHY GIF" } ?: "GIPHY GIF",
                previewUrl = preview,
                fullUrl = full,
                provider = GifProvider.GIPHY
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private suspend fun fetchTenor(query: String): List<GifItem> = try {
        val response = if (query.isBlank()) {
            tenorService.featured(apiKey = tenorKey)
        } else {
            tenorService.search(apiKey = tenorKey, query = query)
        }
        response.results.mapNotNull { item ->
            val preview = item.mediaFormats?.tinygif?.url
                ?: item.mediaFormats?.mediumgif?.url
                ?: item.mediaFormats?.gif?.url
                ?: return@mapNotNull null
            val full = item.mediaFormats?.gif?.url ?: preview
            GifItem(
                id = "tenor_${item.id}",
                title = item.contentDescription?.ifBlank { item.title } ?: "Tenor GIF",
                previewUrl = preview,
                fullUrl = full,
                provider = GifProvider.TENOR
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private suspend fun fetchKlipy(query: String): List<GifItem> = try {
        val response = if (query.isBlank()) {
            klipyService.featured(apiKey = klipyKey)
        } else {
            klipyService.search(apiKey = klipyKey, query = query)
        }
        response.results.mapNotNull { item ->
            val preview = item.mediaFormats?.tinygif?.url
                ?: item.mediaFormats?.mediumgif?.url
                ?: item.mediaFormats?.gif?.url
                ?: return@mapNotNull null
            val full = item.mediaFormats?.gif?.url ?: preview
            GifItem(
                id = "klipy_${item.id}",
                title = item.contentDescription?.ifBlank { item.title } ?: "KLIPY GIF",
                previewUrl = preview,
                fullUrl = full,
                provider = GifProvider.KLIPY
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private val reactionCategories = listOf(
        "happy", "wave", "laugh", "hug", "thumbsup", "dance",
        "wink", "smile", "clap", "blush", "pat", "highfive",
        "think", "shrug", "cry", "sleep", "nod", "poke", "smug", "facepalm"
    )

    private fun resolveReactionCategories(query: String): List<String> {
        if (query.isBlank()) return listOf("happy", "wave", "laugh")
        val q = query.lowercase()
        val matched = when {
            q.contains("hi") || q.contains("hello") || q.contains("hey") || q.contains("bye") -> listOf("wave", "smile")
            q.contains("love") || q.contains("heart") || q.contains("cute") || q.contains("kiss") -> listOf("hug", "blush")
            q.contains("fun") || q.contains("lol") || q.contains("haha") || q.contains("laugh") -> listOf("laugh", "happy")
            q.contains("yes") || q.contains("ok") || q.contains("good") || q.contains("cool") -> listOf("thumbsup", "nod")
            q.contains("party") || q.contains("celebrate") || q.contains("dance") || q.contains("yay") -> listOf("dance", "clap")
            q.contains("sad") || q.contains("cry") || q.contains("sorry") -> listOf("cry", "hug")
            q.contains("sleep") || q.contains("tired") || q.contains("night") -> listOf("sleep", "yawn")
            q.contains("think") || q.contains("what") || q.contains("hmm") -> listOf("think", "shrug")
            else -> reactionCategories.filter { it.contains(q) }.ifEmpty { listOf("happy", "thumbsup") }
        }
        return matched.take(2)
    }

    private suspend fun fetchReactions(query: String): List<GifItem> = try {
        coroutineScope {
            val categories = resolveReactionCategories(query)
            val responses = categories.map { cat ->
                async {
                    val res = nekosService.fetchReactionGifs(category = cat, amount = 8)
                    res.results.mapIndexedNotNull { idx, item ->
                        val url = item.url ?: return@mapIndexedNotNull null
                        GifItem(
                            id = "react_${cat}_${idx}_${url.hashCode()}",
                            title = item.animeName?.ifBlank { cat.replaceFirstChar { it.uppercase() } }
                                ?: cat.replaceFirstChar { it.uppercase() },
                            previewUrl = url,
                            fullUrl = url,
                            provider = GifProvider.REACTIONS
                        )
                    }
                }
            }
            responses.flatMap { it.await() }.distinctBy { it.fullUrl }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
