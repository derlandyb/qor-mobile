import SwiftUI

struct DescriptionSectionView: View {
    let description: String?

    var body: some View {
        if let description, !description.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Sobre o Evento")
                    .font(.headline)
                Text(description)
                    .font(.body)
            }
            .padding()
        }
    }
}
