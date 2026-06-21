package fr.ghez.demineur.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import fr.ghez.demineur.ui.theme.Win95
import fr.ghez.demineur.ui.theme.win95Bevel

/** Segments a..g for each decimal digit (true = lit). Order: a,b,c,d,e,f,g. */
private val DIGIT_SEGMENTS = arrayOf(
    booleanArrayOf(true, true, true, true, true, true, false),    // 0
    booleanArrayOf(false, true, true, false, false, false, false), // 1
    booleanArrayOf(true, true, false, true, true, false, true),    // 2
    booleanArrayOf(true, true, true, true, false, false, true),    // 3
    booleanArrayOf(false, true, true, false, false, true, true),   // 4
    booleanArrayOf(true, false, true, true, false, true, true),    // 5
    booleanArrayOf(true, false, true, true, true, true, true),     // 6
    booleanArrayOf(true, true, true, false, false, false, false),  // 7
    booleanArrayOf(true, true, true, true, true, true, true),      // 8
    booleanArrayOf(true, true, true, true, false, true, true),     // 9
)

/** A 3-digit red seven-segment readout, used for the mine counter and the timer. */
@Composable
fun LedDisplay(
    value: Int,
    modifier: Modifier = Modifier,
    digits: Int = 3,
) {
    Box(
        modifier = modifier
            .win95Bevel(raised = false, thickness = 1.dp)
            .padding(1.dp),
    ) {
        Canvas(
            modifier = Modifier
                .width((digits * 13 + 4).dp)
                .height(23.dp),
        ) {
            drawRect(Win95.LedBackground)

            // Clamp to the representable range and render an optional leading minus sign.
            val clamped = value.coerceIn(-(pow10(digits - 1) - 1), pow10(digits) - 1)
            val negative = clamped < 0
            val chars = IntArray(digits)
            var magnitude = if (negative) -clamped else clamped
            for (i in digits - 1 downTo 0) {
                chars[i] = magnitude % 10
                magnitude /= 10
            }

            val pad = 2.dp.toPx()
            val gap = 2.dp.toPx()
            val digitWidth = (size.width - pad * 2 - gap * (digits - 1)) / digits
            val digitHeight = size.height - pad * 2

            for (i in 0 until digits) {
                val left = pad + i * (digitWidth + gap)
                val showMinus = negative && i == 0
                if (showMinus) {
                    drawDigitSegments(
                        segments = booleanArrayOf(false, false, false, false, false, false, true),
                        left = left, top = pad, w = digitWidth, h = digitHeight,
                    )
                } else {
                    drawDigitSegments(DIGIT_SEGMENTS[chars[i]], left, pad, digitWidth, digitHeight)
                }
            }
        }
    }
}

private fun pow10(n: Int): Int {
    var r = 1
    repeat(n) { r *= 10 }
    return r
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDigitSegments(
    segments: BooleanArray,
    left: Float,
    top: Float,
    w: Float,
    h: Float,
) {
    val t = w * 0.20f          // segment thickness
    val midY = top + (h - t) / 2f
    fun seg(on: Boolean, x: Float, y: Float, sw: Float, sh: Float) {
        drawRect(if (on) Win95.LedOn else Win95.LedOff, Offset(x, y), Size(sw, sh))
    }
    // a top, g middle, d bottom (horizontal)
    seg(segments[0], left + t, top, w - 2 * t, t)
    seg(segments[6], left + t, midY, w - 2 * t, t)
    seg(segments[3], left + t, top + h - t, w - 2 * t, t)
    // f top-left, b top-right (upper verticals)
    val upperY = top + t
    val upperH = midY - upperY
    seg(segments[5], left, upperY, t, upperH)
    seg(segments[1], left + w - t, upperY, t, upperH)
    // e bottom-left, c bottom-right (lower verticals)
    val lowerY = midY + t
    val lowerH = (top + h - t) - lowerY
    seg(segments[4], left, lowerY, t, lowerH)
    seg(segments[2], left + w - t, lowerY, t, lowerH)
}
