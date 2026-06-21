package fr.ghez.demineur.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import fr.ghez.demineur.R
import fr.ghez.demineur.game.Difficulty

@Composable
fun CustomGameDialog(
    current: Difficulty,
    onConfirm: (Difficulty) -> Unit,
    onDismiss: () -> Unit,
) {
    var rows by remember { mutableStateOf(current.rows.toString()) }
    var cols by remember { mutableStateOf(current.cols.toString()) }
    var mines by remember { mutableStateOf(current.mines.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_title)) },
        text = {
            Column {
                NumberField(stringResource(R.string.custom_rows), rows) { rows = it }
                NumberField(stringResource(R.string.custom_cols), cols) { cols = it }
                NumberField(stringResource(R.string.custom_mines), mines) { mines = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    Difficulty.custom(
                        rows = rows.toIntOrNull() ?: current.rows,
                        cols = cols.toIntOrNull() ?: current.cols,
                        mines = mines.toIntOrNull() ?: current.mines,
                    ),
                )
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onChange(input.filter(Char::isDigit).take(3)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
