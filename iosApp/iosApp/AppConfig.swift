import Foundation

/// Points at the API exposed by the root `docker-compose.yml` `api` service (`QOR_API_PORT`, default 8080).
enum AppConfig {
    static let apiBaseUrl = "http://localhost:8080"
}
