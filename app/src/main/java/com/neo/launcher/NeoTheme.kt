package com.neo.launcher

import android.graphics.Color
import android.graphics.Typeface

/** Cele 4 moduri vizuale ale launcher-ului. */
data class NeoTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val bg: Int,
    val primary: Int,
    val secondary: Int,
    val text: Int,
    val dim: Int,
    val typeface: Typeface
) {
    companion object {
        val CYBER = NeoTheme(
            "cyberpunk", "Cyberpunk Neon", "Grilă synthwave, glitch, linii de scanare",
            Color.parseColor("#0B0016"), Color.parseColor("#FF2BD6"), Color.parseColor("#00F0FF"),
            Color.parseColor("#FDF2FF"), Color.parseColor("#A68BC4"),
            Typeface.create("sans-serif-condensed", Typeface.BOLD)
        )
        val HOLO = NeoTheme(
            "hologram", "Hologramă de sticlă", "Sfere de sticlă, valuri de lumină, particule",
            Color.parseColor("#06101F"), Color.parseColor("#8FD3FF"), Color.parseColor("#C49BFF"),
            Color.parseColor("#F2F8FF"), Color.parseColor("#8AA3C2"),
            Typeface.create("sans-serif-light", Typeface.NORMAL)
        )
        val HUD = NeoTheme(
            "hud", "HUD spațial", "Salt prin stele, radar, interfață de navă",
            Color.parseColor("#000814"), Color.parseColor("#FFB000"), Color.parseColor("#4CC9F0"),
            Color.parseColor("#FFF4DC"), Color.parseColor("#7F93A8"),
            Typeface.MONOSPACE
        )
        val MATRIX = NeoTheme(
            "matrix", "Matrix digital", "Ploaie de cod, terminal verde",
            Color.parseColor("#000000"), Color.parseColor("#00FF41"), Color.parseColor("#7DFF9B"),
            Color.parseColor("#D9FFE2"), Color.parseColor("#3F8F55"),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        )
        val ALL = listOf(CYBER, HOLO, HUD, MATRIX)
        fun byId(id: String?): NeoTheme = ALL.firstOrNull { it.id == id } ?: CYBER
    }
}
