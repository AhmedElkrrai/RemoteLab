package com.saif.remotelab.ui.theme

import androidx.compose.ui.graphics.Color

object Palette {
    val Bg = Color(0xFF0F172A)
    val Deep = Color(0xFF020617)
    val Card = Color(0xFF1E293B)
    val Line = Color(0xFF334155)
    val Text = Color(0xFFF8FAFC)
    val TextSoft = Color(0xFFCBD5E1)
    val Muted = Color(0xFF94A3B8)
    val Brand = Color(0xFF38BDF8)
    val BrandContainer = Color(0xFF0369A1)
    val BrandDark = Color(0xFF0C4A6E)
}

/** [base] is the accent itself, [fg] the lighter shade used for text on a tinted background. */
enum class Tone(val base: Color, val fg: Color) {
    Slate(Color(0xFF64748B), Color(0xFFCBD5E1)),
    Brand(Color(0xFF38BDF8), Color(0xFF7DD3FC)),
    Emerald(Color(0xFF10B981), Color(0xFF6EE7B7)),
    Purple(Color(0xFFA855F7), Color(0xFFD8B4FE)),
    Amber(Color(0xFFF59E0B), Color(0xFFFCD34D)),
    Rose(Color(0xFFF43F5E), Color(0xFFFDA4AF)),
    Cyan(Color(0xFF06B6D4), Color(0xFF67E8F9))
}
