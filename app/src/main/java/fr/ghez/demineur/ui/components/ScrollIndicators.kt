package fr.ghez.demineur.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import fr.ghez.demineur.ui.theme.Win95

/**
 * Win95-style scrollbar thumbs overlaid on a scrollable viewport. Drawn only on the axes that can
 * actually scroll, so the player can see there is more board off-screen even on an untouched grid.
 * Purely visual: it draws no pointer input, so taps fall through to the board below.
 */
@Composable
fun ScrollIndicators(
    vertical: ScrollState,
    horizontal: ScrollState,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val bar = 5.dp.toPx()

        if (vertical.maxValue > 0) {
            val viewport = size.height
            val content = viewport + vertical.maxValue
            val thumbH = (viewport / content) * viewport
            val thumbY = (viewport - thumbH) * (vertical.value.toFloat() / vertical.maxValue)
            val x = size.width - bar
            drawScrollbar(x, 0f, bar, viewport, thumbX = x, thumbY = thumbY, thumbW = bar, thumbH = thumbH)
        }

        if (horizontal.maxValue > 0) {
            val viewport = size.width
            val content = viewport + horizontal.maxValue
            val thumbW = (viewport / content) * viewport
            val thumbX = (viewport - thumbW) * (horizontal.value.toFloat() / horizontal.maxValue)
            val y = size.height - bar
            drawScrollbar(0f, y, viewport, bar, thumbX = thumbX, thumbY = y, thumbW = thumbW, thumbH = bar)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScrollbar(
    trackX: Float,
    trackY: Float,
    trackW: Float,
    trackH: Float,
    thumbX: Float,
    thumbY: Float,
    thumbW: Float,
    thumbH: Float,
) {
    // Sunken track
    drawRect(Win95.Shadow, Offset(trackX, trackY), Size(trackW, trackH))
    // Raised thumb (light top-left, dark bottom-right)
    drawRect(Win95.Face, Offset(thumbX, thumbY), Size(thumbW, thumbH))
    val t = 1.5f
    drawRect(Win95.Highlight, Offset(thumbX, thumbY), Size(thumbW, t))
    drawRect(Win95.Highlight, Offset(thumbX, thumbY), Size(t, thumbH))
    drawRect(Win95.DarkShadow, Offset(thumbX, thumbY + thumbH - t), Size(thumbW, t))
    drawRect(Win95.DarkShadow, Offset(thumbX + thumbW - t, thumbY), Size(t, thumbH))
}
