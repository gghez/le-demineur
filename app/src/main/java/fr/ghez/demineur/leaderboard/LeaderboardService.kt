package fr.ghez.demineur.leaderboard

import android.app.Activity
import fr.ghez.demineur.game.Difficulty
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over an online leaderboard. The game never depends on it being available: every
 * method is best-effort and failures degrade silently to local-only play. Methods that need a UI
 * context take an [Activity], because Play Games clients are activity-scoped.
 */
interface LeaderboardService {

    /** Whether the platform leaderboard is usable at all (e.g. Play Games present on device). */
    val isAvailable: Boolean

    /** Reactive sign-in state, for showing the right affordance in the UI. */
    val signedIn: StateFlow<Boolean>

    /** Attempt a silent/automatic sign-in. Safe to call from `onResume`. */
    fun ensureSignedIn(activity: Activity)

    /** Trigger an interactive sign-in (e.g. after the user taps "Connexion"). */
    fun signInInteractively(activity: Activity)

    /** Submit a winning [seconds] time for a ranked difficulty. No-op for custom/unranked. */
    fun submitScore(activity: Activity, difficulty: Difficulty, seconds: Int)

    /** Open the native leaderboard UI for [difficulty], if one exists. */
    fun showLeaderboard(activity: Activity, difficulty: Difficulty)
}
