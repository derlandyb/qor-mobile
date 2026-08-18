package com.qualorock.android.filters

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.qualorock.shared.filters.DateBucket
import com.qualorock.shared.filters.FilterChip
import org.junit.Rule
import org.junit.Test

class ActiveFilterChipsRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_one_chip_per_active_filter_type_when_the_row_renders_then_each_chip_is_shown() {
        val chips = listOf(FilterChip.DateChip(DateBucket.HOJE), FilterChip.CityChip("Vila Velha"))

        composeTestRule.setContent {
            ActiveFilterChipsRow(chips = chips, onRemove = {}, onClearAll = {})
        }

        composeTestRule.onNodeWithText("Hoje").assertExists()
        composeTestRule.onNodeWithText("Vila Velha").assertExists()
    }

    @Test
    fun given_a_chip_when_its_remove_affordance_is_tapped_then_onRemove_is_invoked_with_only_that_chip() {
        val dateChip = FilterChip.DateChip(DateBucket.HOJE)
        var removed: FilterChip? = null

        composeTestRule.setContent {
            ActiveFilterChipsRow(chips = listOf(dateChip), onRemove = { removed = it }, onClearAll = {})
        }

        composeTestRule.onNodeWithContentDescription("Remover filtro Hoje").performClick()

        assert(removed == dateChip)
    }

    @Test
    fun given_active_filters_when_limpar_filtros_is_tapped_then_onClearAll_is_invoked() {
        var cleared = false

        composeTestRule.setContent {
            ActiveFilterChipsRow(
                chips = listOf(FilterChip.CityChip("Vila Velha")),
                onRemove = {},
                onClearAll = { cleared = true },
            )
        }

        composeTestRule.onNodeWithText("Limpar filtros").performClick()

        assert(cleared)
    }
}
