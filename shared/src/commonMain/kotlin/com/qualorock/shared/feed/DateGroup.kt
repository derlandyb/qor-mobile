package com.qualorock.shared.feed

import com.qualorock.shared.domain.Event

data class DateGroup(
    val label: String,
    val events: List<Event>,
)
