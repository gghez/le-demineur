package fr.ghez.demineur.leaderboard

import android.app.Activity
import fr.ghez.demineur.game.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fallback used when no online leaderboard is available. Every call is a silent no-op. */
object NoopLeaderboard : LeaderboardService {
    override val isAvailable: Boolean = false
    override val signedIn: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override fun ensureSignedIn(activity: Activity) = Unit
    override fun signInInteractively(activity: Activity) = Unit
    override fun submitScore(activity: Activity, difficulty: Difficulty, seconds: Int) = Unit
    override fun showLeaderboard(activity: Activity, difficulty: Difficulty) = Unit
}
