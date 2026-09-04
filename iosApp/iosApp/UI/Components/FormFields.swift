import SwiftUI
import shared

/// I6 — the design system's text field variant with pt-BR inline validation error display,
/// consumed by every auth screen (login/signup/reset). `errorMessage == nil` means valid/untouched.
struct QorTextField: View {
    @Binding var value: String
    let label: String
    let errorMessage: String?
    var keyboardType: UIKeyboardType = .default
    var isSecure: Bool = false
    var trailingIcon: (() -> AnyView)?

    var body: some View {
        VStack(alignment: .leading, spacing: QorSpace.space1) {
            HStack {
                Group {
                    if isSecure {
                        SecureField(label, text: $value)
                    } else {
                        TextField(label, text: $value)
                    }
                }
                .keyboardType(keyboardType)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()

                if let trailingIcon {
                    trailingIcon()
                }
            }
            .padding(QorSpace.space3)
            .overlay(
                RoundedRectangle(cornerRadius: QorRadius.radiusMd)
                    .stroke(
                        errorMessage != nil ? QorColor.danger : QorColor.accentBlue,
                        lineWidth: CGFloat(QualORockThemeTokens.shared.BorderWidthHairlineDp)
                    )
            )

            if let errorMessage {
                Text(errorMessage)
                    .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                    .foregroundStyle(QorColor.danger)
            }
        }
    }
}

/// I6 — email field variant, keyboard type + label fixed.
struct EmailField: View {
    @Binding var value: String
    let errorMessage: String?

    var body: some View {
        QorTextField(
            value: $value, label: String(localized: "field_label_email"), errorMessage: errorMessage,
            keyboardType: .emailAddress
        )
    }
}

/// 6-digit OTP code field (AUTH-10), matching Android's `OtpCodeField`/`qor-website`'s
/// `OtpCodeInput` behavior: non-digit characters are stripped and input is capped at
/// `otpCodeMaxLength` as it's typed, rather than relying on validation alone to catch a
/// too-long paste.
private let otpCodeMaxLength = 6

/// Strips non-digit characters and caps the result at [otpCodeMaxLength] — pure, so it's
/// unit-testable without SwiftUI (mirrors Android's inline `OtpCodeField` filter).
func sanitizeOtpInput(_ raw: String) -> String {
    String(raw.filter(\.isNumber).prefix(otpCodeMaxLength))
}

struct OtpCodeField: View {
    @Binding var value: String
    let errorMessage: String?

    var body: some View {
        QorTextField(
            value: Binding(get: { value }, set: { value = sanitizeOtpInput($0) }),
            label: String(localized: "field_label_code"), errorMessage: errorMessage, keyboardType: .numberPad
        )
    }
}

/// I6 — password field variant, with a "Mostrar senha"/"Ocultar senha" visibility toggle.
struct PasswordField: View {
    @Binding var value: String
    let errorMessage: String?
    @State private var revealed = false

    var body: some View {
        QorTextField(
            value: $value, label: String(localized: "field_label_password"), errorMessage: errorMessage,
            isSecure: !revealed,
            trailingIcon: {
                let toggleLabel = revealed
                    ? String(localized: "password_toggle_hide")
                    : String(localized: "password_toggle_show")
                return AnyView(
                    Button(toggleLabel) { revealed.toggle() }
                        .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                        .foregroundStyle(QorColor.accentBlue)
                )
            }
        )
    }
}
