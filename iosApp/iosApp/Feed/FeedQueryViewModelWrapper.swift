import Shared
import SwiftUI

@MainActor
final class FeedQueryViewModelWrapper: ObservableObject {
    @Published private(set) var query: String = ""
    @Published private(set) var resultsState: FeedResultsUiState = FeedResultsUiStateInactive.shared
    @Published private(set) var filterState: FilterState = FilterState(
        dateBucket: nil,
        city: nil,
        genres: [],
        artist: nil
    )
    @Published private(set) var genreOptions: OptionsUiState = OptionsUiStateLoading.shared
    @Published private(set) var artistOptions: OptionsUiState = OptionsUiStateLoading.shared

    private let iosViewModel: IosFeedQueryViewModel
    private var resultsHandle: Closeable?
    private var filterHandle: Closeable?
    private var genreOptionsHandle: Closeable?
    private var artistOptionsHandle: Closeable?

    init(baseUrl: String) {
        iosViewModel = IosFeedQueryViewModel(baseUrl: baseUrl)

        resultsHandle = iosViewModel.watchResults { [weak self] newState in
            DispatchQueue.main.async { self?.resultsState = newState }
        }
        filterHandle = iosViewModel.watchFilterState { [weak self] newState in
            DispatchQueue.main.async { self?.filterState = newState }
        }
        genreOptionsHandle = iosViewModel.watchGenreOptions { [weak self] newState in
            DispatchQueue.main.async { self?.genreOptions = newState }
        }
        artistOptionsHandle = iosViewModel.watchArtistOptions { [weak self] newState in
            DispatchQueue.main.async { self?.artistOptions = newState }
        }
    }

    deinit {
        resultsHandle?.close()
        filterHandle?.close()
        genreOptionsHandle?.close()
        artistOptionsHandle?.close()
        iosViewModel.close()
    }

    func setQuery(_ query: String) {
        self.query = query
        iosViewModel.setQuery(query: query)
    }

    func clearQuery() {
        setQuery("")
    }

    func selectDateBucket(_ bucket: DateBucket?) {
        iosViewModel.filterViewModel.selectDateBucket(bucket: bucket)
    }

    func selectCity(city: String?) {
        iosViewModel.filterViewModel.selectCity(city: city)
    }

    func toggleGenre(genre: String) {
        iosViewModel.filterViewModel.toggleGenre(genre: genre)
    }

    func selectArtist(_ artist: ArtistOption?) {
        iosViewModel.filterViewModel.selectArtist(artist: artist)
    }

    func removeChip(_ chip: FilterChip) {
        iosViewModel.filterViewModel.removeChip(chip: chip)
    }

    func clearAll() {
        iosViewModel.filterViewModel.clearAll()
    }

    func retryResults() {
        iosViewModel.retryResults()
    }
}
