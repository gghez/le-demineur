package fr.ghez.demineur.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.ghez.demineur.R
import fr.ghez.demineur.game.Difficulty
import fr.ghez.demineur.ui.components.LedDisplay
import fr.ghez.demineur.ui.components.MenuBar
import fr.ghez.demineur.ui.components.MineGrid
import fr.ghez.demineur.ui.components.SmileyButton
import fr.ghez.demineur.ui.components.TitleBar
import fr.ghez.demineur.ui.theme.Win95
import fr.ghez.demineur.ui.theme.win95Bevel

@Composable
fun GameScreen(
    state: GameUiState,
    signedIn: Boolean,
    onCellTap: (Int, Int) -> Unit,
    onCellLongPress: (Int, Int) -> Unit,
    onSmiley: () -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onShowLeaderboard: () -> Unit,
    onSignIn: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Win95.Desktop)
            .win95Bevel(raised = true, thickness = 3.dp)
            .padding(3.dp),
    ) {
        TitleBar(title = stringResource(R.string.window_title))

        Box {
            MenuBar(
                gameLabel = stringResource(R.string.menu_game),
                helpLabel = stringResource(R.string.menu_help),
                onGameClick = { menuOpen = true },
                onHelpClick = { onShowLeaderboard() },
            )
            GameMenu(
                expanded = menuOpen,
                signedIn = signedIn,
                onDismiss = { menuOpen = false },
                onNewGame = { onSmiley() },
                onSelectDifficulty = onSelectDifficulty,
                onCustom = { showCustomDialog = true },
                onShowLeaderboard = onShowLeaderboard,
                onSignIn = onSignIn,
            )
        }

        Spacer(Modifier.height(4.dp))
        StatusPanel(
            minesRemaining = state.minesRemaining,
            elapsedSeconds = state.elapsedSeconds,
            status = state.status,
            onSmiley = onSmiley,
        )
        Spacer(Modifier.height(4.dp))

        // Board area: sunken frame holding a board that scrolls when larger than the screen.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .win95Bevel(raised = false, thickness = 3.dp)
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
            ) {
                MineGrid(
                    board = state.board,
                    onCellTap = onCellTap,
                    onCellLongPress = onCellLongPress,
                )
            }
        }
    }

    if (showCustomDialog) {
        CustomGameDialog(
            current = state.difficulty,
            onConfirm = {
                showCustomDialog = false
                onSelectDifficulty(it)
            },
            onDismiss = { showCustomDialog = false },
        )
    }
}

@Composable
private fun StatusPanel(
    minesRemaining: Int,
    elapsedSeconds: Int,
    status: fr.ghez.demineur.game.GameStatus,
    onSmiley: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .win95Bevel(raised = false, thickness = 3.dp)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LedDisplay(value = minesRemaining)
        SmileyButton(status = status, onClick = onSmiley)
        LedDisplay(value = elapsedSeconds)
    }
}

@Composable
private fun GameMenu(
    expanded: Boolean,
    signedIn: Boolean,
    onDismiss: () -> Unit,
    onNewGame: () -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onCustom: () -> Unit,
    onShowLeaderboard: () -> Unit,
    onSignIn: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(Win95.Face),
    ) {
        fun choose(action: () -> Unit) {
            onDismiss()
            action()
        }
        DropdownMenuItem(text = { Text(stringResource(R.string.action_new_game)) }, onClick = { choose(onNewGame) })
        HorizontalDivider()
        DropdownMenuItem(text = { Text(stringResource(R.string.action_beginner)) }, onClick = { choose { onSelectDifficulty(Difficulty.Beginner) } })
        DropdownMenuItem(text = { Text(stringResource(R.string.action_intermediate)) }, onClick = { choose { onSelectDifficulty(Difficulty.Intermediate) } })
        DropdownMenuItem(text = { Text(stringResource(R.string.action_expert)) }, onClick = { choose { onSelectDifficulty(Difficulty.Expert) } })
        DropdownMenuItem(text = { Text(stringResource(R.string.action_custom)) }, onClick = { choose(onCustom) })
        HorizontalDivider()
        DropdownMenuItem(text = { Text(stringResource(R.string.action_leaderboard)) }, onClick = { choose(onShowLeaderboard) })
        if (!signedIn) {
            DropdownMenuItem(text = { Text(stringResource(R.string.action_sign_in)) }, onClick = { choose(onSignIn) })
        }
    }
}
