package com.qualorock.shared.filters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterModelsTest {
    @Test
    fun `given a filter state with nothing selected when isEmpty is checked then it is true`() {
        assertTrue(FilterState().isEmpty)
    }

    @Test
    fun `given a filter state with any field selected when isEmpty is checked then it is false`() {
        assertTrue(!FilterState(dateBucket = DateBucket.HOJE).isEmpty)
    }

    @Test
    fun `given no active filters when asChips is called then it returns an empty list`() {
        assertTrue(FilterState().asChips().isEmpty())
    }

    @Test
    fun `given one active filter per type when asChips is called then it returns one chip per type`() {
        val state =
            FilterState(
                dateBucket = DateBucket.HOJE,
                city = "Vila Velha",
                genres = setOf("Rock", "Samba"),
                artist = ArtistOption(id = "1", name = "Jorge & the Band"),
            )

        val chips = state.asChips()

        assertEquals(4, chips.size)
        assertTrue(chips.any { it is FilterChip.DateChip && it.bucket == DateBucket.HOJE })
        assertTrue(chips.any { it is FilterChip.CityChip && it.city == "Vila Velha" })
        assertTrue(chips.any { it is FilterChip.GenreChip && it.genres == setOf("Rock", "Samba") })
        assertTrue(chips.any { it is FilterChip.ArtistChip && it.artist.id == "1" })
    }

    @Test
    fun `given multiple selected genres when asChips is called then they collapse into a single genre chip`() {
        val state = FilterState(genres = setOf("Rock", "Samba", "Punk"))

        val chips = state.asChips()

        assertEquals(1, chips.size)
        assertTrue(chips.single() is FilterChip.GenreChip)
    }
}
