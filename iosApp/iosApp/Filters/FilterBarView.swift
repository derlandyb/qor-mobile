import Shared
import SwiftUI

private let cityOptions = ["Vitória", "Vila Velha", "Serra", "Cariacica"]

struct FilterBarView: View {
    let state: FilterState
    let onDateSelect: (DateBucket?) -> Void
    let onCitySelect: (String?) -> Void
    let onOpenPanel: () -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(DateBucket.entries, id: \.self) { bucket in
                    chip(label: bucket.label, selected: state.dateBucket == bucket) {
                        onDateSelect(state.dateBucket == bucket ? nil : bucket)
                    }
                }
                ForEach(cityOptions, id: \.self) { city in
                    chip(label: city, selected: state.city == city) {
                        onCitySelect(state.city == city ? nil : city)
                    }
                }
                Button(action: onOpenPanel) {
                    Label("Filtros", systemImage: "slider.horizontal.3")
                }
            }
            .padding(.horizontal)
        }
    }

    private func chip(label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(selected ? Color.accentColor : Color(.systemGray5))
                .foregroundColor(selected ? .white : .primary)
                .clipShape(Capsule())
        }
    }
}
