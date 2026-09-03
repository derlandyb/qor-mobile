package design

/**
 * NIGHTLIFE-GV design tokens, ported verbatim from `design-system.md` §2-3 as
 * framework-agnostic Kotlin constants.
 *
 * Deliberately framework-agnostic: colors are plain ARGB [Long] hex values (NOT
 * `androidx.compose.ui.graphics.Color`), so this file has zero Android/iOS dependency and can
 * live in `shared`'s domain-adjacent `design` package per Clean Architecture's
 * zero-framework-dependency rule (ARCHITECTURE §8.5). Android UI converts these to
 * `androidx.compose.ui.graphics.Color(NightlifeGvTokens.ColorBgDeep)`; iOS UI converts them to
 * `Color(hex:)`/`UIColor` equivalents. Both conversions happen in platform UI code, not here.
 */
@Suppress("MagicNumber")
object NightlifeGvTokens {

    // region 2.1 Global Color Palette — base neutrals (ARGB Long, alpha = 0xFF)

    /** `--color-bg-deep` #0B0D14 — app background, deepest layer. */
    const val ColorBgDeep: Long = 0xFF0B0D14

    /** `--color-bg-base` #12141D — section background, below cards. */
    const val ColorBgBase: Long = 0xFF12141D

    /** `--color-surface-card` #1B1E29 — card surfaces. */
    const val ColorSurfaceCard: Long = 0xFF1B1E29

    /** `--color-surface-card-hover` #232733 — card hover surface. */
    const val ColorSurfaceCardHover: Long = 0xFF232733

    /** `--color-border-subtle` #2A2E3B — hairline borders, dividers. */
    const val ColorBorderSubtle: Long = 0xFF2A2E3B

    /** `--color-text-primary` #F5F6FA — titles, primary text. */
    const val ColorTextPrimary: Long = 0xFFF5F6FA

    /** `--color-text-secondary` #9A9FB0 — metadata, dates, venue sub-text. */
    const val ColorTextSecondary: Long = 0xFF9A9FB0

    /** `--color-text-tertiary` #666B7D — disabled, timestamps, fine print. */
    const val ColorTextTertiary: Long = 0xFF666B7D

    // endregion

    // region 2.1 Vibrant Accents

    /** `--accent-pink` #FF2E7E — primary CTA, "live now" pulse, Sertanejo tag. */
    const val AccentPink: Long = 0xFFFF2E7E

    /** `--accent-orange` #FF8A1E — secondary CTA, Rock tag, date badge. */
    const val AccentOrange: Long = 0xFFFF8A1E

    /** `--accent-purple` #B14EFF — Eletrônico tag, hover glows, focus rings. */
    const val AccentPurple: Long = 0xFFB14EFF

    /** `--accent-blue` #2EC5FF — Reggae tag, links, map icon. */
    const val AccentBlue: Long = 0xFF2EC5FF

    // endregion

    // region 2.1 Semantic aliases

    /** `--color-success` = `--accent-blue` — "Confirmed" / open now. */
    const val ColorSuccess: Long = AccentBlue

    /** `--color-live` = `--accent-pink` — "Ao vivo agora" pulse dot. */
    const val ColorLive: Long = AccentPink

    /** `--color-danger` #FF4D4D — sold-out / cancelled (info only, no checkout). */
    const val ColorDanger: Long = 0xFFFF4D4D

    // endregion

    // region 2.2 Typography Scale

    const val FontFamilyDisplay: String = "Space Grotesk"
    const val FontFamilyBody: String = "Inter"

    /** `--text-event-title` — Space Grotesk 700, 22px / 1.15 line-height, -0.01em tracking. */
    object TextEventTitle {
        const val FontFamily: String = FontFamilyDisplay
        const val Weight: Int = 700
        const val SizeSp: Int = 22
        const val LineHeightMultiplier: Double = 1.15
        const val LetterSpacingEm: Double = -0.01
    }

    /** `--text-event-title-lg` — Space Grotesk 700, 32px / 1.1, -0.015em — event detail H1. */
    object TextEventTitleLg {
        const val FontFamily: String = FontFamilyDisplay
        const val Weight: Int = 700
        const val SizeSp: Int = 32
        const val LineHeightMultiplier: Double = 1.1
        const val LetterSpacingEm: Double = -0.015
    }

    /** `--text-venue-name` — Inter 600, 15px / 1.3, 0 tracking. */
    object TextVenueName {
        const val FontFamily: String = FontFamilyBody
        const val Weight: Int = 600
        const val SizeSp: Int = 15
        const val LineHeightMultiplier: Double = 1.3
        const val LetterSpacingEm: Double = 0.0
    }

    /** `--text-city-label` — Inter 600, 12px / 1.2, 0.02em, uppercase. */
    object TextCityLabel {
        const val FontFamily: String = FontFamilyBody
        const val Weight: Int = 600
        const val SizeSp: Int = 12
        const val LineHeightMultiplier: Double = 1.2
        const val LetterSpacingEm: Double = 0.02
        const val Uppercase: Boolean = true
    }

    /** `--text-metadata` — Inter 500, 13px / 1.4, 0 — date, time, price/cover info. */
    object TextMetadata {
        const val FontFamily: String = FontFamilyBody
        const val Weight: Int = 500
        const val SizeSp: Int = 13
        const val LineHeightMultiplier: Double = 1.4
        const val LetterSpacingEm: Double = 0.0
    }

    /** `--text-badge` — Space Grotesk 600, 11px / 1, 0.04em, uppercase — genre/city badge text. */
    object TextBadge {
        const val FontFamily: String = FontFamilyDisplay
        const val Weight: Int = 600
        const val SizeSp: Int = 11
        const val LineHeightMultiplier: Double = 1.0
        const val LetterSpacingEm: Double = 0.04
        const val Uppercase: Boolean = true
    }

    /** `--text-body` — Inter 400, 14px / 1.5, 0 — descriptions. */
    object TextBody {
        const val FontFamily: String = FontFamilyBody
        const val Weight: Int = 400
        const val SizeSp: Int = 14
        const val LineHeightMultiplier: Double = 1.5
        const val LetterSpacingEm: Double = 0.0
    }

    /** `--text-button` — Space Grotesk 600, 14px / 1, 0.01em — CTA button label. */
    object TextButton {
        const val FontFamily: String = FontFamilyDisplay
        const val Weight: Int = 600
        const val SizeSp: Int = 14
        const val LineHeightMultiplier: Double = 1.0
        const val LetterSpacingEm: Double = 0.01
    }

    // endregion

    // region 2.3 Spacing & Borders (8px base grid)

    /** `--space-1` — 4px, micro, icon-to-text gap only. */
    const val Space1Dp: Int = 4

    /** `--space-2` — 8px. */
    const val Space2Dp: Int = 8

    /** `--space-3` — 16px. Card internal padding; mobile grid gutter. */
    const val Space3Dp: Int = 16

    /** `--space-4` — 24px. Desktop grid gutter. */
    const val Space4Dp: Int = 24

    /** `--space-5` — 32px. */
    const val Space5Dp: Int = 32

    /** `--space-6` — 48px. Section vertical rhythm between major sections. */
    const val Space6Dp: Int = 48

    /** `--space-7` — 64px. */
    const val Space7Dp: Int = 64

    /** `--radius-sm` — 6px. Badges, tags, chips. */
    const val RadiusSmDp: Int = 6

    /** `--radius-md` — 12px. Buttons, inputs. */
    const val RadiusMdDp: Int = 12

    /** `--radius-lg` — 16px. Event cards. */
    const val RadiusLgDp: Int = 16

    /** `--radius-image` — 14px. Flyer/image holder (2px less than card, nests cleanly). */
    const val RadiusImageDp: Int = 14

    /** `--radius-pill` — 999px. Filter bar badges. */
    const val RadiusPillDp: Int = 999

    /** `--border-width-hairline` — 1px. Card borders, dividers. */
    const val BorderWidthHairlineDp: Int = 1

    // endregion

    // region 3. Interaction & Animation Spec

    /** `--duration-fast` — 150ms. Icon/badge state change. */
    const val DurationFastMs: Int = 150

    /** `--duration-base` — 250ms. Card hover, button hover. */
    const val DurationBaseMs: Int = 250

    /** `--duration-slow` — 450ms. Entrance animation, page transitions. */
    const val DurationSlowMs: Int = 450

    /** `--duration-stagger` — 60ms. Delay increment per card in a grid entrance. */
    const val DurationStaggerMs: Int = 60

    /**
     * Cartesian control points of a CSS `cubic-bezier(x1, y1, x2, y2)` easing curve.
     *
     * Both Compose's `androidx.compose.animation.core.CubicBezierEasing(x1, y1, x2, y2)` and
     * SwiftUI's `.animation(.timingCurve(x1, y1, x2, y2, duration:))` consume these four floats
     * directly, so platform UI code never re-derives or hardcodes the curve.
     */
    data class CubicBezierEasing(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
    )

    /** `--ease-beat` `cubic-bezier(0.34, 1.56, 0.64, 1)` — hover scale, entrance pop (overshoot). */
    val EaseBeat: CubicBezierEasing = CubicBezierEasing(x1 = 0.34f, y1 = 1.56f, x2 = 0.64f, y2 = 1f)

    /** `--ease-smooth` `cubic-bezier(0.4, 0, 0.2, 1)` — color/gradient shifts, opacity. */
    val EaseSmooth: CubicBezierEasing = CubicBezierEasing(x1 = 0.4f, y1 = 0f, x2 = 0.2f, y2 = 1f)

    // endregion
}
