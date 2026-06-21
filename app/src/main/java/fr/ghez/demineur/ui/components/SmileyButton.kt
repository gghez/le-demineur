package fr.ghez.demineur.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.ghez.demineur.R
import fr.ghez.demineur.game.GameStatus
import fr.ghez.demineur.ui.theme.Win95
import fr.ghez.demineur.ui.theme.win95Bevel

private val FaceYellow = Color(0xFFFFFF00)

@Composable
fun SmileyButton(
    status: GameStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val description = stringResource(R.string.cd_smiley)

    Canvas(
        modifier = modifier
            .size(34.dp)
            .win95Bevel(raised = !pressed, thickness = 2.dp, face = Win95.Face)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        val offset = if (pressed) 1.dp.toPx() else 0f
        val inset = size.minDimension * 0.18f + offset
        val faceTopLeft = Offset(inset, inset)
        val faceSize = Size(size.width - inset * 2, size.height - inset * 2)
        val cx = faceTopLeft.x + faceSize.width / 2
        val cy = faceTopLeft.y + faceSize.height / 2
        val r = faceSize.width / 2

        drawCircle(FaceYellow, r, Offset(cx, cy))
        drawCircle(Color.Black, r, Offset(cx, cy), style = Stroke(width = 2f))

        val eyeY = cy - r * 0.20f
        val eyeDx = r * 0.40f
        val eyeR = r * 0.10f

        when (status) {
            GameStatus.WON -> {
                // Sunglasses bar.
                drawRect(
                    Color.Black,
                    topLeft = Offset(cx - r * 0.62f, eyeY - r * 0.12f),
                    size = Size(r * 1.24f, r * 0.28f),
                )
                smile(cx, cy, r)
            }
            GameStatus.LOST -> {
                // X eyes + frown.
                drawX(cx - eyeDx, eyeY, eyeR * 1.6f)
                drawX(cx + eyeDx, eyeY, eyeR * 1.6f)
                frown(cx, cy, r)
            }
            else -> {
                drawCircle(Color.Black, eyeR, Offset(cx - eyeDx, eyeY))
                drawCircle(Color.Black, eyeR, Offset(cx + eyeDx, eyeY))
                smile(cx, cy, r)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.smile(cx: Float, cy: Float, r: Float) {
    drawArc(
        color = Color.Black,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.5f, cy - r * 0.35f),
        size = Size(r, r * 0.7f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.frown(cx: Float, cy: Float, r: Float) {
    drawArc(
        color = Color.Black,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.5f, cy + r * 0.05f),
        size = Size(r, r * 0.7f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawX(cx: Float, cy: Float, half: Float) {
    val s = Stroke(width = 2.5f, cap = StrokeCap.Round)
    drawLine(Color.Black, Offset(cx - half, cy - half), Offset(cx + half, cy + half), strokeWidth = s.width, cap = StrokeCap.Round)
    drawLine(Color.Black, Offset(cx + half, cy - half), Offset(cx - half, cy + half), strokeWidth = s.width, cap = StrokeCap.Round)
}
