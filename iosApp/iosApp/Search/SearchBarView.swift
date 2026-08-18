import SwiftUI

struct SearchBarView: View {
    let query: String
    let onQueryChange: (String) -> Void
    let onClear: () -> Void

    var body: some View {
        HStack {
            TextField("O que você quer ouvir?", text: Binding(get: { query }, set: onQueryChange))
                .textFieldStyle(.roundedBorder)

            if !query.isEmpty {
                Button(action: onClear) {
                    Image(systemName: "xmark.circle.fill")
                }
                .accessibilityLabel("Limpar busca")
            }
        }
        .padding(.horizontal)
    }
}
