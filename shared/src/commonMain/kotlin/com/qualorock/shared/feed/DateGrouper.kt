package com.qualorock.shared.feed

import com.qualorock.shared.domain.Event
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

/**
 * Groups events by calendar day in pt-BR, per FEED design's "Hoje"/"Amanhã"/weekday-labelled buckets.
 * Assumes [events] already arrive sorted soonest-first from the API.
 */
object DateGrouper {
    private val weekdayAbbreviations =
        mapOf(
            DayOfWeek.MONDAY to "Seg",
            DayOfWeek.TUESDAY to "Ter",
            DayOfWeek.WEDNESDAY to "Qua",
            DayOfWeek.THURSDAY to "Qui",
            DayOfWeek.FRIDAY to "Sex",
            DayOfWeek.SATURDAY to "Sáb",
            DayOfWeek.SUNDAY to "Dom",
        )

    private val monthAbbreviations =
        mapOf(
            1 to "Jan", 2 to "Fev", 3 to "Mar", 4 to "Abr", 5 to "Mai", 6 to "Jun",
            7 to "Jul", 8 to "Ago", 9 to "Set", 10 to "Out", 11 to "Nov", 12 to "Dez",
        )

    fun group(
        events: List<Event>,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        today: LocalDate = Clock.System.todayIn(timeZone),
    ): List<DateGroup> =
        events
            .groupBy { it.startDateTime.toLocalDateTime(timeZone).date }
            .entries
            .sortedBy { it.key }
            .map { (date, eventsOnDate) -> DateGroup(label = labelFor(date, today), events = eventsOnDate) }

    private fun labelFor(
        date: LocalDate,
        today: LocalDate,
    ): String {
        val daysFromToday = date.toEpochDays() - today.toEpochDays()
        return when (daysFromToday) {
            0 -> "Hoje"
            1 -> "Amanhã"
            else -> "${weekdayAbbreviations.getValue(date.dayOfWeek)}, ${date.dayOfMonth} ${monthAbbreviations.getValue(date.monthNumber)}"
        }
    }
}
