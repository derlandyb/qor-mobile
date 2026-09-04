import SwiftUI
import shared

/// I5 — required, non-pre-checked consent acceptance (AUTH-02/AUTH-03), shared shape reused
/// across signup screens. `accepted` is hoisted — the caller owns whether it's persisted/reset.
struct ConsentCapture: View {
    @Binding var accepted: Bool

    var body: some View {
        HStack(alignment: .center, spacing: QorSpace.space2) {
            Toggle(isOn: $accepted) { EmptyView() }
                .labelsHidden()
                .toggleStyle(ConsentCheckboxToggleStyle())
                .accessibilityIdentifier("consent_checkbox")

            Text(String(localized: "consent_terms_text"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextBody.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)
        }
        .padding(.vertical, QorSpace.space2)
    }
}

/// A checkbox (square) rather than the default switch, matching Android's `Checkbox`.
private struct ConsentCheckboxToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            Image(systemName: configuration.isOn ? "checkmark.square.fill" : "square")
                .foregroundStyle(configuration.isOn ? QorColor.accentPink : QorColor.textSecondary)
        }
        .buttonStyle(.plain)
    }
}
