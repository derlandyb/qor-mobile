import MapKit
import Shared
import SwiftUI

/// Fixed default camera extent covering Vitória, Vila Velha, Serra, and Cariacica — never device
/// location (MAP-001/005).
let grandeVitoriaRegion = MKCoordinateRegion(
    center: CLLocationCoordinate2D(latitude: -20.35, longitude: -40.3),
    span: MKCoordinateSpan(latitudeDelta: 0.5, longitudeDelta: 0.4)
)

/// The Map tab (MAP-001/005): a Grande-Vitória-wide, zero-permission view of upcoming events, applying the
/// same filter state as Feed (lifted above this screen — see `SharedFilterOwner`).
struct MapScreen: View {
    @ObservedObject var viewModel: MapQueryViewModelWrapper
    let onOpenEventDetails: (String) -> Void
    let onClearFilters: () -> Void

    @State private var previewEvent: Event?
    @State private var clusterEvents: [Event]?

    var body: some View {
        ZStack {
            content
        }
        .sheet(isPresented: previewBinding) {
            if let event = previewEvent {
                MarkerPreviewSheet(event: event) { selected in
                    previewEvent = nil
                    onOpenEventDetails(selected.id)
                }
            }
        }
        .sheet(isPresented: clusterBinding) {
            if let events = clusterEvents {
                MultiEventListSheet(events: events) { selected in
                    clusterEvents = nil
                    previewEvent = selected
                }
            }
        }
    }

    private var previewBinding: Binding<Bool> {
        Binding(get: { previewEvent != nil }, set: { if !$0 { previewEvent = nil } })
    }

    private var clusterBinding: Binding<Bool> {
        Binding(get: { clusterEvents != nil }, set: { if !$0 { clusterEvents = nil } })
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.markersState {
        case is MapMarkersUiStateLoading:
            ProgressView()
        case let errorState as MapMarkersUiStateError:
            errorView(message: errorState.message)
        case let loaded as MapMarkersUiStateLoaded:
            loadedContent(loaded)
        default:
            EmptyView()
        }
    }

    @ViewBuilder
    private func loadedContent(_ loaded: MapMarkersUiStateLoaded) -> some View {
        MapViewRepresentable(
            events: loaded.markers,
            region: grandeVitoriaRegion,
            onSelectSingle: { previewEvent = $0 },
            onSelectCluster: { clusterEvents = $0 }
        )
        .ignoresSafeArea(edges: .bottom)

        // MapKit doesn't expose a Swift-visible "current visible bounds" without a delegate round-trip
        // (unlike Android's derivedStateOf on cameraPositionState.projection), so this only distinguishes
        // "zero markers fetched" with vs. without active filters — the unit-testable equivalent lives in
        // shared/commonMain's MapDisplayStateDeriver, exercised there rather than duplicated here.
        if loaded.markers.isEmpty {
            noResultsBanner(activeFilters: loaded.activeFilters)
        }
    }

    @ViewBuilder
    private func noResultsBanner(activeFilters: FilterState) -> some View {
        VStack(spacing: 12) {
            if activeFilters.isEmpty {
                Text("Nenhum evento nesta área.")
            } else {
                Text("Nenhum evento corresponde aos filtros ativos.")
                Button("Limpar filtros", action: onClearFilters)
            }
        }
        .padding()
        .background(.thinMaterial)
        .cornerRadius(12)
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Text(message)
                .font(.headline)
            Button("Tentar novamente") { viewModel.retry() }
        }
    }
}
