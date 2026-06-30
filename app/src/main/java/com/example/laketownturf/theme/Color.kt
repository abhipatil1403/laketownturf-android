package com.example.laketownturf.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════
// DARK PALETTE
// ══════════════════════════════════════════════════════════
val DarkNavy = Color(0xFF0D0D1A)
val DarkNavySurface = Color(0xFF141428)
val DarkNavyVariant = Color(0xFF0A0A14)
val CardBackgroundDark = Color(0xFF1A1A30)
val CardBorderDark = Color(0xFF252545)

// ══════════════════════════════════════════════════════════
// LIGHT PALETTE
// ══════════════════════════════════════════════════════════
val LightBackground = Color(0xFFF7F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightVariant = Color(0xFFEDEDF3)
val CardBackgroundLight = Color(0xFFFFFFFF)
val CardBorderLight = Color(0xFFE0E0EA)

// ══════════════════════════════════════════════════════════
// SHARED ACCENT COLORS (same in both themes)
// ══════════════════════════════════════════════════════════

// Greens
val PrimaryGreen = Color(0xFF2ECC71)
val DarkGreen = Color(0xFF1B8C52)
val GreenTint = Color(0xFFD4EDDA)
val GreenSurface = Color(0xFF1A3A2A)

// Amber CTA
val AmberCTA = Color(0xFFF5A623)
val AmberDark = Color(0xFFD4901F)
val AmberLight = Color(0xFFFFF3E0)

// Danger
val DangerRed = Color(0xFFE74C3C)
val DangerRedDark = Color(0xFFC0392B)
val DangerSurface = Color(0xFF3A1A1A)

// ══════════════════════════════════════════════════════════
// TEXT (kept for backward-compat, screens should migrate to colorScheme)
// ══════════════════════════════════════════════════════════
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0B0)
val TextTertiary = Color(0xFF707070)
val TextOnPrimary = Color(0xFFFFFFFF)

val TextPrimaryLight = Color(0xFF1A1A2E)
val TextSecondaryLight = Color(0xFF6B6B80)
val TextTertiaryLight = Color(0xFF9E9EB0)

// Misc
val ShimmerBase = Color(0xFF2A2A4A)
val ShimmerHighlight = Color(0xFF3A3A5A)
val Divider = Color(0xFF252545)

// Legacy aliases – screens being migrated can still reference these
val CardBackground = CardBackgroundDark
val CardBorder = CardBorderDark
