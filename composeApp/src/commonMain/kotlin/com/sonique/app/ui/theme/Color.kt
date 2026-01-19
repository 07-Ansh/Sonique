package com.sonique.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// Base Palette
// ============================================

val musica_accent = Color(0xFFFFFFFF)  // White accent - modern, clean
val musica_black = Color(0xFF000000)
val musica_dark_grey = Color(0xFF1C1C1E)
val musica_light_grey = Color(0xFF2C2C2E)
val musica_grey_text = Color(0xFFB3B3B3)
val musica_white = Color(0xFFFFFFFF)

// ============================================
// Semantic Colors - Backgrounds
// ============================================

val backgroundPrimary = musica_dark_grey       // #1C1C1E - Main screen background
val backgroundElevated = musica_light_grey     // #2C2C2E - Elevated surfaces
val backgroundCard = Color(0xFF242424)         // Cards, dialogs

// ============================================
// Semantic Colors - Overlays
// ============================================

val overlayLight = Color(0x33000000)      // 20% black - subtle overlay
val overlayMedium = Color(0x80000000)     // 50% black - medium overlay
val overlayHeavy = Color(0xBF000000)      // 75% black - heavy overlay

// Legacy overlay colors (still used in player screens)
val overlay = Color(0x32242424)           // ~20% overlay - legacy
val blackMoreOverlay = Color(0x8f242424)  // ~56% overlay - legacy

// ============================================
// Semantic Colors - Text
// ============================================

val textPrimary = musica_white            // #FFFFFF - Primary text
val textSecondary = musica_grey_text      // #B3B3B3 - Secondary text
val textDisabled = Color(0x61FFFFFF)      // 38% white - Disabled text
val textHighEmphasis = Color(0xC4FFFFFF)  // 77% white - High emphasis

// ============================================
// Semantic Colors - Interactive States
// ============================================

val surfaceHover = Color(0x14FFFFFF)      // 8% white - Hover state
val surfacePressed = Color(0x1FFFFFFF)    // 12% white - Pressed state
val surfaceSelected = Color(0x33FFFFFF)   // 20% white - Selected state

// ============================================
// Material3 Theme Colors
// ============================================

val md_theme_dark_primary = musica_accent
val md_theme_dark_onPrimary = musica_black
val md_theme_dark_primaryContainer = Color(0xFF424242)  // Medium grey for filled buttons
val md_theme_dark_onPrimaryContainer = musica_white

val md_theme_dark_secondary = musica_accent
val md_theme_dark_onSecondary = musica_black
val md_theme_dark_secondaryContainer = backgroundElevated
val md_theme_dark_onSecondaryContainer = musica_white

val md_theme_dark_tertiary = musica_accent
val md_theme_dark_onTertiary = musica_black
val md_theme_dark_tertiaryContainer = backgroundElevated
val md_theme_dark_onTertiaryContainer = musica_white

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

val md_theme_dark_background = backgroundPrimary
val md_theme_dark_onBackground = textPrimary

val md_theme_dark_surface = backgroundPrimary
val md_theme_dark_onSurface = textPrimary

val md_theme_dark_surfaceVariant = backgroundElevated
val md_theme_dark_onSurfaceVariant = textSecondary

val md_theme_dark_outline = textSecondary
val md_theme_dark_inverseOnSurface = musica_black
val md_theme_dark_inverseSurface = musica_white
val md_theme_dark_inversePrimary = musica_black
val md_theme_dark_shadow = musica_black
val md_theme_dark_surfaceTint = musica_accent
val md_theme_dark_outlineVariant = backgroundElevated
val md_theme_dark_scrim = musica_black

// ============================================
// Legacy/Specific Use Colors
// ============================================

val colorPrimaryDark = Color(0x19000000)      // 10% black tint
val back_button_color = Color(0x197E7E7E)    // 10% grey tint
val checkedFilterColor = Color(0xFF4D4848)   // Filter chip background

// Shimmer effect
val shimmerBackground = Color(0x7E383737)    // 49% dark grey
val shimmerLine = Color(0xFF4D4848)          // Shimmer animation line

// Utility colors
val seed = musica_accent
val bottomBarSeedDark = musica_accent
val white = Color(0xFFFFFFFF)
val transparent = Color(0x00000000)

