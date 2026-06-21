package fr.ghez.demineur

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import fr.ghez.demineur.leaderboard.LeaderboardService
import fr.ghez.demineur.leaderboard.PlayGamesLeaderboard
import fr.ghez.demineur.ui.GameEvent
import fr.ghez.demineur.ui.GameScreen
import fr.ghez.demineur.ui.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()
    private val leaderboard: LeaderboardService by lazy { PlayGamesLeaderboard(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.uiState.collectAsState()
                val signedIn by leaderboard.signedIn.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is GameEvent.Won -> {
                                leaderboard.submitScore(this@MainActivity, event.difficulty, event.seconds)
                                if (event.isNewRecord) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.new_record, "${event.seconds} s"),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        }
                    }
                }

                GameScreen(
                    state = state,
                    signedIn = signedIn,
                    onCellTap = viewModel::onCellTap,
                    onCellLongPress = viewModel::onCellLongPress,
                    onSmiley = viewModel::onSmileyTap,
                    onSelectDifficulty = viewModel::newGame,
                    onShowLeaderboard = { leaderboard.showLeaderboard(this@MainActivity, state.difficulty) },
                    onSignIn = { leaderboard.signInInteractively(this@MainActivity) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        leaderboard.ensureSignedIn(this)
    }
}
