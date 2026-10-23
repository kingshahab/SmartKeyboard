package ir.shahab.keyboard

import android.graphics.Color
import android.os.Build
import android.view.WindowInsets

@Suppress("DEPRECATION")
fun bottomInset(i: WindowInsets): Int =
    if (Build.VERSION.SDK_INT >= 30) i.getInsets(WindowInsets.Type.systemBars()).bottom
    else i.systemWindowInsetBottom

@Suppress("DEPRECATION")
fun topInset(i: WindowInsets): Int =
    if (Build.VERSION.SDK_INT >= 30) i.getInsets(WindowInsets.Type.systemBars()).top
    else i.systemWindowInsetTop

class KbTheme(val bg: Int, val key: Int, val text: Int, val special: Int, val accent: Int)

val THEME_NAMES = listOf("روشن", "تیره", "آبی")

val THEMES = listOf(
    KbTheme(Color.parseColor("#ECEFF1"), Color.parseColor("#FFFFFF"), Color.parseColor("#212121"),
        Color.parseColor("#CFD8DC"), Color.parseColor("#90CAF9")),
    KbTheme(Color.parseColor("#1B1B1F"), Color.parseColor("#3A3B40"), Color.parseColor("#F5F5F5"),
        Color.parseColor("#2A2B30"), Color.parseColor("#4FC3F7")),
    KbTheme(Color.parseColor("#0D3B4F"), Color.parseColor("#1C6A87"), Color.parseColor("#FFFFFF"),
        Color.parseColor("#14506B"), Color.parseColor("#FFB300"))
)
