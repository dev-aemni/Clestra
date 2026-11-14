package com.example.data.network

import com.example.BuildConfig
import com.example.data.security.InternalVault
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
    GIPHY("GIPHY", "GIPHY"),
    REACTIONS("Reactions", "REACT"),
    OTAKU("AnimeGIFs", "GIF")
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
data class GiphyRendition(
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GiphyImages(
    @Json(name = "fixed_width") val fixedWidth: GiphyRendition? = null,
    @Json(name = "original") val original: GiphyRendition? = null,
    @Json(name = "downsized") val downsized: GiphyRendition? = null
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

// --- OtakuGIFs Public Reaction GIF DTO ---
@JsonClass(generateAdapter = true)
data class OtakuGifResponse(
    @Json(name = "url") val url: String? = null
)

interface GiphyApiService {
    @GET("v1/gifs/trending")
    suspend fun trending(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 24,
        @Query("rating") rating: String = "pg-13"
    ): GiphyResponse

    @GET("v1/gifs/search")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 24,
        @Query("rating") rating: String = "pg-13"
    ): GiphyResponse
}

interface NekosBestApiService {
    @GET("api/v2/{category}")
    suspend fun fetchReactionGifs(
        @Path("category") category: String,
        @Query("amount") amount: Int = 10
    ): NekosBestResponse
}

interface OtakuGifsApiService {
    @GET("gif")
    suspend fun fetchGif(
        @Query("reaction") reaction: String,
        @Query("format") format: String = "gif"
    ): OtakuGifResponse
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
            .baseUrl(InternalVault.giphyEndpoint())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GiphyApiService::class.java)
    }

    private val nekosService: NekosBestApiService by lazy {
        Retrofit.Builder()
            .baseUrl(InternalVault.nekosEndpoint())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NekosBestApiService::class.java)
    }

    private val otakuService: OtakuGifsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(InternalVault.otakuEndpoint())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OtakuGifsApiService::class.java)
    }

    private val giphyKey: String
        get() = runCatching { BuildConfig.GIPHY_API_KEY }.getOrDefault("")

    fun isProviderConfigured(provider: GifProvider): Boolean = when (provider) {
        GifProvider.ALL, GifProvider.REACTIONS, GifProvider.OTAKU -> true
        GifProvider.GIPHY -> giphyKey.isNotBlank() && giphyKey != "MY_GIPHY_API_KEY"
    }

    suspend fun searchOrTrending(
        provider: GifProvider,
        query: String
    ): GifSearchResult = coroutineScope {
        val cleanQuery = query.trim()
        when (provider) {
            GifProvider.GIPHY -> {
                val items = if (isProviderConfigured(GifProvider.GIPHY)) {
                    fetchGiphy(cleanQuery)
                } else {
                    emptyList()
                }
                GifSearchResult(items = items, notice = null)
            }
            GifProvider.REACTIONS -> {
                GifSearchResult(items = fetchReactions(cleanQuery), notice = null)
            }
            GifProvider.OTAKU -> {
                GifSearchResult(items = fetchOtakuGifs(cleanQuery), notice = null)
            }
            GifProvider.ALL -> {
                val giphyJob = async {
                    if (isProviderConfigured(GifProvider.GIPHY)) fetchGiphy(cleanQuery) else emptyList()
                }
                val reactionsJob = async { fetchReactions(cleanQuery) }
                val otakuJob = async { fetchOtakuGifs(cleanQuery) }

                val combined = buildList {
                    val lists = listOf(giphyJob.await(), reactionsJob.await(), otakuJob.await())
                    val maxLen = lists.maxOfOrNull { it.size } ?: 0
                    for (i in 0 until maxLen) {
                        for (list in lists) {
                            if (i < list.size) add(list[i])
                        }
                    }
                }.distinctBy { it.fullUrl }

                GifSearchResult(items = combined, notice = null)
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
            val preview = item.images?.fixedWidth?.url
                ?: item.images?.downsized?.url
                ?: item.images?.original?.url
                ?: return@mapNotNull null
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

    private suspend fun fetchOtakuGifs(query: String): List<GifItem> = try {
        coroutineScope {
            val categories = resolveReactionCategories(query)
            val targets = (0 until 6).map { idx -> categories[idx % categories.size] }
            val jobs = targets.mapIndexed { idx, reaction ->
                async {
                    val res = runCatching { otakuService.fetchGif(reaction = reaction) }.getOrNull()
                    val url = res?.url ?: return@async null
                    GifItem(
                        id = "otaku_${reaction}_${idx}_${url.hashCode()}",
                        title = reaction.replaceFirstChar { it.uppercase() },
                        previewUrl = url,
                        fullUrl = url,
                        provider = GifProvider.OTAKU
                    )
                }
            }
            jobs.mapNotNull { it.await() }.distinctBy { it.fullUrl }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
