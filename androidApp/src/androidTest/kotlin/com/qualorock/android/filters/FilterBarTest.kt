package com.qualorock.android.filters

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.qualorock.shared.filters.DateBucket
import com.qualorock.shared.filters.FilterState
import org.junit.Rule
import org.junit.Test

class FilterBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_no_active_filters_when_a_date_chip_is_tapped_then_onDateSelect_is_invoked_with_that_bucket() {
        var selected: DateBucket? = null

        composeTestRule.setContent {
            FilterBar(
                state = FilterState(),
                onDateSelect = { selected = it },
                onCitySelect = {},
                onOpenPanel = {},
            )
        }

        composeTestRule.onNodeWithText("Hoje").performClick()

        assert(selected == DateBucket.HOJE)
    }

    @Test
    fun given_no_active_filters_when_a_city_chip_is_tapped_then_onCitySelect_is_invoked_with_that_city() {
        var selected: String? = null

        composeTestRule.setContent {
            FilterBar(
                state = FilterState(),
                onDateSelect = {},
                onCitySelect = { selected = it },
                onOpenPanel = {},
            )
        }

        composeTestRule.onNodeWithText("Vila Velha").performScrollTo().performClick()

        assert(selected == "Vila Velha")
    }

    @Test
    fun given_the_filter_bar_when_the_filtros_button_is_tapped_then_onOpenPanel_is_invoked() {
        var opened = false

        composeTestRule.setContent {
            FilterBar(
                state = FilterState(),
                onDateSelect = {},
                onCitySelect = {},
                onOpenPanel = { opened = true },
            )
        }

        composeTestRule.onNodeWithText("Filtros").performScrollTo().performClick()

        assert(opened)
    }
}
