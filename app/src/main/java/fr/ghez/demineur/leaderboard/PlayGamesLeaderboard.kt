package fr.ghez.demineur.leaderboard

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.games.PlayGames
import fr.ghez.demineur.R
import fr.ghez.demineur.game.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Play Games Services v2 leaderboard. Time scores are submitted in milliseconds against
 * "smaller is better" time-formatted leaderboards (one per ranked difficulty). All calls fail
 * silently so the game keeps working offline or when the user declines sign-in.
 *
 * Requires [com.google.android.gms.games.PlayGamesSdk.initialize] to have run (see DemineurApp),
 * and real leaderboard IDs injected via local.properties (see
 * docs/agent-references/deployment.md) — resolved as build-time `resValue`s.
 */
class PlayGamesLeaderboard(private val appContext: Context) : LeaderboardService {

    private val _signedIn = MutableStateFlow(false)
    override val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()
    override val isAvailable: Boolean = true

    override fun ensureSignedIn(activity: Activity) {
        runCatching {
            PlayGames.getGamesSignInClient(activity).isAuthenticated
                .addOnSuccessListener { result -> _signedIn.value = result.isAuthenticated }
                .addOnFailureListener { _signedIn.value = false }
        }.onFailure { Log.w(TAG, "isAuthenticated failed", it) }
    }

    override fun signInInteractively(activity: Activity) {
        runCatching {
            PlayGames.getGamesSignInClient(activity).signIn()
                .addOnCompleteListener { task ->
                    _signedIn.value = task.isSuccessful && task.result.isAuthenticated
                }
        }.onFailure { Log.w(TAG, "signIn failed", it) }
    }

    override fun submitScore(activity: Activity, difficulty: Difficulty, seconds: Int) {
        if (!difficulty.isRanked) return
        val leaderboardId = leaderboardIdFor(difficulty) ?: return
        runCatching {
            PlayGames.getLeaderboardsClient(activity)
                .submitScore(leaderboardId, seconds * 1000L)
        }.onFailure { Log.w(TAG, "submitScore failed", it) }
    }

    override fun showLeaderboard(activity: Activity, difficulty: Difficulty) {
        val leaderboardId = leaderboardIdFor(difficulty) ?: return
        runCatching {
            PlayGames.getLeaderboardsClient(activity).getLeaderboardIntent(leaderboardId)
                .addOnSuccessListener { intent -> activity.startActivity(intent) }
        }.onFailure { Log.w(TAG, "showLeaderboard failed", it) }
    }

    /** Resolves a difficulty to its configured leaderboard id, or null if not configured. */
    private fun leaderboardIdFor(difficulty: Difficulty): String? {
        val resId = when (difficulty.id) {
            Difficulty.Beginner.id -> R.string.leaderboard_beginner
            Difficulty.Intermediate.id -> R.string.leaderboard_intermediate
            Difficulty.Expert.id -> R.string.leaderboard_expert
            else -> return null
        }
        return appContext.getString(resId).takeIf { it.isNotBlank() }
    }

    private companion object {
        const val TAG = "PlayGamesLeaderboard"
    }
}
