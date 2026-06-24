package fr.ghez.demineur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.ghez.demineur.R
import fr.ghez.demineur.ui.components.Win95Button
import fr.ghez.demineur.ui.components.Win95Dialog

/** Explains the touch controls; opened from the "?" menu entry. Styled as a Win95 window. */
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    Win95Dialog(title = stringResource(R.string.help_title), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HelpRow(stringResource(R.string.help_tap_title), stringResource(R.string.help_tap_body))
            HelpRow(stringResource(R.string.help_long_title), stringResource(R.string.help_long_body))
            HelpRow(stringResource(R.string.help_chord_title), stringResource(R.string.help_chord_body))
            HelpRow(stringResource(R.string.help_smiley_title), stringResource(R.string.help_smiley_body))
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Win95Button(text = stringResource(R.string.ok), onClick = onDismiss)
        }
    }
}

@Composable
private fun HelpRow(title: String, body: String) {
    Column {
        androidx.compose.material3.Text(title, color = Color.Black, fontWeight = FontWeight.Bold)
        androidx.compose.material3.Text(body, color = Color.Black)
    }
}
