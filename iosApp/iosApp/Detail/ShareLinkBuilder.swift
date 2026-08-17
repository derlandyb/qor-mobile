/// Builds the canonical share URL — the real, already-shipped `sharing` feature's crawler-aware
/// redirect route (`api`'s `ShareController@resolveEvent`), not a not-yet-built web app route.
enum ShareLinkBuilder {
    static func canonicalURL(forEventId eventId: String, baseUrl: String) -> String {
        "\(baseUrl)/compartilhar/eventos/\(eventId)"
    }
}
