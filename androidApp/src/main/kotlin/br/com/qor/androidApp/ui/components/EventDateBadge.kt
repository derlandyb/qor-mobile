package br.com.qor.androidApp.ui.components

private const val TimeLabelLength = 5

private val PtBrMonthAbbreviations = listOf(
    "JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ",
)

/** design-system.md §4.1's floating date badge — `{month}` (pt-BR, uppercase) / `{day}`. */
data class DateBadgeLabel(val month: String, val day: String)

/**
 * `Event.startsAt` is an ISO-8601 string (`api.md` T25's contract) — parsed by field position
 * rather than a date library, since only the calendar month/day/time are needed here, not
 * timezone-aware arithmetic.
 */
fun formatDateBadge(isoStartsAt: String): DateBadgeLabel {
    val datePart = isoStartsAt.substringBefore('T')
    val (_, monthStr, dayStr) = datePart.split("-")
    return DateBadgeLabel(month = PtBrMonthAbbreviations[monthStr.toInt() - 1], day = dayStr)
}

/** The `{time}` portion of design-system.md §4.1's venue/time row — `HH:mm`, 24h, no timezone conversion. */
fun formatEventTime(isoStartsAt: String): String {
    val timePart = isoStartsAt.substringAfter('T').substringBefore('Z')
    return timePart.take(TimeLabelLength)
}
