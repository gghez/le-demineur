package fr.ghez.demineur.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.ghez.demineur.ui.theme.Win95
import fr.ghez.demineur.ui.theme.win95Bevel

/** Navy title bar with the window title and decorative window controls. */
@Composable
fun TitleBar(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
        WindowButton("_")
        Spacer(Modifier.width(2.dp))
        WindowButton("□")
        Spacer(Modifier.width(2.dp))
        WindowButton("✕")
    }
}

@Composable
private fun WindowButton(symbol: String) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .win95Bevel(raised = true, thickness = 1.dp, face = Win95.Face),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/** Menu bar with the classic "Partie" and "?" entries. */
@Composable
fun MenuBar(
    gameLabel: String,
    helpLabel: String,
    onGameClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Win95.Face)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MenuLabel(gameLabel, onGameClick)
        MenuLabel(helpLabel, onHelpClick)
    }
}

@Composable
private fun MenuLabel(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.Black,
        fontSize = 13.sp,
        fontFamily = FontFamily.SansSerif,
        modifier = Modifier
            .clickableNoRipple(onClick)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** A click with no ripple/highlight, matching the flat Win95 menu look. */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
