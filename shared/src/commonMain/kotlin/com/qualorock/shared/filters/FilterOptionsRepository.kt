package com.qualorock.shared.filters

import com.qualorock.shared.data.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
private data class GenreOptionsResponse(val data: List<String>)

@Serializable
private data class ArtistOptionsResponse(val data: List<ArtistOption>)

interface FilterOptionsRepository {
    suspend fun getGenreOptions(): Result<List<String>>

    suspend fun getArtistOptions(): Result<List<ArtistOption>>
}

class KtorFilterOptionsRepository(
    private val baseUrl: String,
    private val httpClient: HttpClient = createHttpClient(),
) : FilterOptionsRepository {
    override suspend fun getGenreOptions(): Result<List<String>> =
        runCatching {
            httpClient.get("$baseUrl/api/filter-options/genres").body<GenreOptionsResponse>().data
        }

    override suspend fun getArtistOptions(): Result<List<ArtistOption>> =
        runCatching {
            httpClient.get("$baseUrl/api/filter-options/artists").body<ArtistOptionsResponse>().data
        }
}
