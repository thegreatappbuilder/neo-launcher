package com.neo.launcher

import android.graphics.Color
import android.graphics.Typeface

/** Modurile vizuale ale launcher-ului. */
data class NeoTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val bg: Int,
    val primary: Int,
    val secondary: Int,
    val text: Int,
    val dim: Int,
    val typeface: Typeface,
    /** Rotunjirea colțurilor pentru casete și butoane (dp). */
    val radius: Float = 8f,
    /** Iconițe în cerc în loc de pătrat rotunjit. */
    val roundIcons: Boolean = false
) {
    companion object {
        private fun c(hex: String) = Color.parseColor(hex)

        val CYBER = NeoTheme(
            "cyberpunk", "Cyberpunk Neon", "Grilă synthwave, glitch, linii de scanare",
            c("#0B0016"), c("#FF2BD6"), c("#00F0FF"), c("#FDF2FF"), c("#A68BC4"),
            Typeface.create("sans-serif-condensed", Typeface.BOLD), radius = 4f
        )
        val HOLO = NeoTheme(
            "hologram", "Hologramă de sticlă", "Sfere de sticlă, valuri de lumină, particule",
            c("#06101F"), c("#8FD3FF"), c("#C49BFF"), c("#F2F8FF"), c("#8AA3C2"),
            Typeface.create("sans-serif-light", Typeface.NORMAL), radius = 22f, roundIcons = true
        )
        val HUD = NeoTheme(
            "hud", "HUD spațial", "Salt prin stele, radar, interfață de navă",
            c("#000814"), c("#FFB000"), c("#4CC9F0"), c("#FFF4DC"), c("#7F93A8"),
            Typeface.MONOSPACE, radius = 0f
        )
        val MATRIX = NeoTheme(
            "matrix", "Matrix digital", "Ploaie de cod, terminal verde",
            c("#000000"), c("#00FF41"), c("#7DFF9B"), c("#D9FFE2"), c("#3F8F55"),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD), radius = 2f
        )
        val AURORA = NeoTheme(
            "aurora", "Aurora boreală", "Perdele de lumină polară peste munți",
            c("#020B16"), c("#3DFFB0"), c("#B07CFF"), c("#EFFFF8"), c("#7FA6A0"),
            Typeface.create("sans-serif-light", Typeface.NORMAL), radius = 18f
        )
        val INFERNO = NeoTheme(
            "inferno", "Infern de lavă", "Scântei, magmă incandescentă, val de căldură",
            c("#120202"), c("#FF6A00"), c("#FFD23F"), c("#FFF1E0"), c("#B07A62"),
            Typeface.create("sans-serif-condensed", Typeface.BOLD), radius = 6f
        )
        val OCEAN = NeoTheme(
            "ocean", "Adâncul oceanului", "Raze de lumină, bule, meduze luminoase",
            c("#001424"), c("#2EE6FF"), c("#7B8CFF"), c("#E6FBFF"), c("#6D97A8"),
            Typeface.create("sans-serif", Typeface.NORMAL), radius = 24f, roundIcons = true
        )
        val GALAXY = NeoTheme(
            "galaxy", "Galaxie spirală", "Mii de stele care se rotesc în jurul nucleului",
            c("#03010A"), c("#FFC46B"), c("#9D7BFF"), c("#FFF6E8"), c("#9A8FB5"),
            Typeface.create("serif", Typeface.NORMAL), radius = 14f, roundIcons = true
        )
        val CIRCUIT = NeoTheme(
            "circuit", "Circuit cuantic", "Trasee electronice și impulsuri de energie",
            c("#00080A"), c("#00FFD5"), c("#FF3D81"), c("#E2FFFA"), c("#5F918A"),
            Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL), radius = 0f
        )
        val STORM = NeoTheme(
            "storm", "Furtună electrică", "Nori, ploaie și fulgere la atingere",
            c("#05070D"), c("#9CC9FF"), c("#E5F0FF"), c("#EEF4FF"), c("#7A879C"),
            Typeface.create("sans-serif-medium", Typeface.NORMAL), radius = 8f
        )
        val CITY = NeoTheme(
            "neoncity", "Oraș neon", "Zgârie-nori, ferestre aprinse, mașini zburătoare",
            c("#07031A"), c("#FF4FD8"), c("#46E8FF"), c("#FFF0FB"), c("#9A86B8"),
            Typeface.create("sans-serif-condensed", Typeface.NORMAL), radius = 4f
        )
        val FROST = NeoTheme(
            "frost", "Cristale de gheață", "Ninsoare, fulgi de cristal, aer polar",
            c("#071528"), c("#BDEBFF"), c("#7FB4FF"), c("#F5FCFF"), c("#8DA6BD"),
            Typeface.create("sans-serif-thin", Typeface.NORMAL), radius = 16f
        )
        val SAKURA = NeoTheme(
            "sakura", "Sakura la lună plină", "Petale de cireș, lună, liniște zen",
            c("#140A1E"), c("#FF9EC7"), c("#FFE2B8"), c("#FFF3F8"), c("#B391A8"),
            Typeface.create("serif", Typeface.ITALIC), radius = 28f, roundIcons = true
        )
        val PLEXUS = NeoTheme(
            "plexus", "Constelație", "Rețea de particule conectate care urmărește degetul",
            c("#04030E"), c("#7CF9FF"), c("#FF7BEA"), c("#F1F0FF"), c("#8C89B2"),
            Typeface.create("sans-serif-light", Typeface.NORMAL), radius = 12f
        )

        val ALL = listOf(
            CYBER, HOLO, HUD, MATRIX,
            AURORA, INFERNO, OCEAN, GALAXY, CIRCUIT, STORM, CITY, FROST, SAKURA, PLEXUS
        )

        fun byId(id: String?): NeoTheme = ALL.firstOrNull { it.id == id } ?: CYBER
    }
}
