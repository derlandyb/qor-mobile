package com.qualorock.shared.filters

import kotlinx.serialization.Serializable

enum class DateBucket(val wireValue: String, val label: String) {
    HOJE("hoje", "Hoje"),
    AMANHA("amanha", "Amanhã"),
    FIM_DE_SEMANA("fim_de_semana", "Este fim de semana"),
    PROXIMA_SEMANA("proxima_semana", "Próxima semana"),
}

@Serializable
data class ArtistOption(val id: String, val name: String)

data class FilterState(
    val dateBucket: DateBucket? = null,
    val city: String? = null,
    val genres: Set<String> = emptySet(),
    val artist: ArtistOption? = null,
) {
    val isEmpty: Boolean get() = dateBucket == null && city == null && genres.isEmpty() && artist == null

    /** One removable chip per active filter type — genres collapse to a single chip listing count. */
    fun asChips(): List<FilterChip> =
        listOfNotNull(
            dateBucket?.let { FilterChip.DateChip(it) },
            city?.let { FilterChip.CityChip(it) },
            genres.takeIf { it.isNotEmpty() }?.let { FilterChip.GenreChip(it) },
            artist?.let { FilterChip.ArtistChip(it) },
        )
}

sealed interface FilterChip {
    data class DateChip(val bucket: DateBucket) : FilterChip

    data class CityChip(val city: String) : FilterChip

    data class GenreChip(val genres: Set<String>) : FilterChip

    data class ArtistChip(val artist: ArtistOption) : FilterChip
}
