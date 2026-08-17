import Shared
import SwiftUI

struct PromoterSectionView: View {
    let promoter: Promoter?

    var body: some View {
        if let promoter {
            HStack(spacing: 8) {
                Text(promoter.name).font(.headline)
                if promoter.verificationStatus == VerificationStatus.verified {
                    Image(systemName: "checkmark.seal.fill").foregroundColor(.accentColor)
                }
                if let instagram = promoter.socialLinks?["instagram"], let url = URL(string: instagram) {
                    Link(destination: url) { Image(systemName: "camera") }
                }
                if let whatsapp = promoter.socialLinks?["whatsapp"], let url = URL(string: whatsapp) {
                    Link(destination: url) { Image(systemName: "message") }
                }
                Spacer()
            }
            .padding()
        }
    }
}
