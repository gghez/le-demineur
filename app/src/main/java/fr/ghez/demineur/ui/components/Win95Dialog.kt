package fr.ghez.demineur.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.ghez.demineur.ui.theme.Win95
import fr.ghez.demineur.ui.theme.win95Bevel

/**
 * A modal styled as a Windows 95 window: raised gray frame, navy title bar with a close box, and
 * a gray body. Use [Win95Button] for the actions so the whole popup matches the game chrome.
 */
@Composable
fun Win95Dialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = modifier
                .padding(24.dp)
                .widthIn(max = 380.dp)
                .win95Bevel(raised = true, thickness = 3.dp)
                .padding(3.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(Win95.TitleBar)
                    .padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = Win95.TitleText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                )
                Spacer(Modifier.weight(1f))
                CloseBox(onDismiss)
            }
            Column(modifier = Modifier.padding(12.dp)) { content() }
        }
    }
}

@Composable
private fun CloseBox(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Canvas(
        modifier = Modifier
            .size(16.dp)
            .win95Bevel(raised = true, thickness = 1.dp, face = Win95.Face)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        val s = size.minDimension
        val w = (s * 0.10f).coerceAtLeast(2.5f)
        val p = s * 0.32f
        drawLine(Color.Black, Offset(p, p), Offset(s - p, s - p), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(Color.Black, Offset(s - p, p), Offset(p, s - p), strokeWidth = w, cap = StrokeCap.Round)
    }
}

/** A raised Win95 push-button that sinks while pressed. */
@Composable
fun Win95Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .win95Bevel(raised = !pressed, thickness = 2.dp, face = Win95.Face)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}
