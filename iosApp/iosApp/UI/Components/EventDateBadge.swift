import Foundation

/// design-system.md §4.1's floating date badge — `{month}` (pt-BR, uppercase) / `{day}`.
/// Mirrors Android's `EventDateBadge.kt` (A2) — same pt-BR month-abbreviation table, same
/// field-position ISO-8601 parsing (only calendar month/day/time are needed, not
/// timezone-aware arithmetic).
struct DateBadgeLabel: Equatable {
    let month: String
    let day: String
}

private let ptBrMonthAbbreviations = [
    "JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
]

private let timeLabelLength = 5

/// `Event.startsAt` is an ISO-8601 string (`api.md` T25's contract) — parsed by field position
/// rather than a date library, since only the calendar month/day/time are needed here.
func formatDateBadge(isoStartsAt: String) -> DateBadgeLabel {
    let datePart = isoStartsAt.split(separator: "T", maxSplits: 1).first.map(String.init) ?? isoStartsAt
    let components = datePart.split(separator: "-")
    guard components.count == 3, let monthIndex = Int(components[1]), monthIndex >= 1, monthIndex <= 12 else {
        return DateBadgeLabel(month: "", day: "")
    }
    return DateBadgeLabel(month: ptBrMonthAbbreviations[monthIndex - 1], day: String(components[2]))
}

/// The `{time}` portion of design-system.md §4.1's venue/time row — `HH:mm`, 24h, no timezone conversion.
func formatEventTime(isoStartsAt: String) -> String {
    guard let afterT = isoStartsAt.split(separator: "T", maxSplits: 1).last else { return "" }
    let beforeZ = afterT.split(separator: "Z", maxSplits: 1).first.map(String.init) ?? String(afterT)
    return String(beforeZ.prefix(timeLabelLength))
}
