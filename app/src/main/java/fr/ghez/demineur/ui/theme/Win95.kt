package fr.ghez.demineur.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The fixed Windows 95 system palette used throughout the UI. */
object Win95 {
    val Face = Color(0xFFC0C0C0)        // ButtonFace (silver)
    val Highlight = Color(0xFFFFFFFF)   // ButtonHighlight (white)
    val Shadow = Color(0xFF808080)      // ButtonShadow (gray)
    val DarkShadow = Color(0xFF000000)  // ButtonDarkShadow (black)
    val Desktop = Color(0xFF008080)     // classic teal desktop
    val TitleBar = Color(0xFF000080)    // active title bar (navy)
    val TitleText = Color(0xFFFFFFFF)

    val LedOn = Color(0xFFFF0000)
    val LedOff = Color(0xFF3B0000)
    val LedBackground = Color(0xFF000000)

    /** Mine-count digit colors, indexed by the number 1..8 (index 0 unused). */
    val numberColors = arrayOf(
        Color(0xFF000000), // 0 (unused)
        Color(0xFF0000FF), // 1 blue
        Color(0xFF008000), // 2 green
        Color(0xFFFF0000), // 3 red
        Color(0xFF000080), // 4 navy
        Color(0xFF800000), // 5 maroon
        Color(0xFF008080), // 6 teal
        Color(0xFF000000), // 7 black
        Color(0xFF808080), // 8 gray
    )
}

/**
 * Draws the classic two-tone 3D border behind a composable.
 * [raised] true = button/tile pops out (light top-left, dark bottom-right);
 * false = sunken/inset (dark top-left, light bottom-right).
 */
fun Modifier.win95Bevel(
    raised: Boolean = true,
    thickness: Dp = 2.dp,
    face: Color? = Win95.Face,
): Modifier = this.drawBehind {
    face?.let { drawRect(it) }
    val t = thickness.toPx()
    val w = size.width
    val h = size.height
    val topLeft = if (raised) Win95.Highlight else Win95.Shadow
    val bottomRight = if (raised) Win95.Shadow else Win95.Highlight

    // top + left
    drawRect(topLeft, Offset(0f, 0f), Size(w, t))
    drawRect(topLeft, Offset(0f, 0f), Size(t, h))
    // bottom + right
    drawRect(bottomRight, Offset(0f, h - t), Size(w, t))
    drawRect(bottomRight, Offset(w - t, 0f), Size(t, h))
}

/** Convenience: a raised bevel plus inner padding, for panels and buttons. */
fun Modifier.win95Panel(thickness: Dp = 2.dp): Modifier =
    this.win95Bevel(raised = true, thickness = thickness).padding(thickness)
