package fr.ghez.demineur.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.ghez.demineur.game.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.scoreDataStore: DataStore<Preferences> by preferencesDataStore(name = "scores")

/** Persists best completion times (in seconds) per difficulty on the device. */
class ScoreRepository(private val context: Context) {

    private fun key(difficultyId: String) = intPreferencesKey("best_$difficultyId")

    /** Best times keyed by [Difficulty.id]; absent entries mean "no record yet". */
    fun bestTimes(): Flow<Map<String, Int>> = context.scoreDataStore.data.map { prefs ->
        buildMap {
            for (preset in Difficulty.presets) {
                prefs[key(preset.id)]?.let { put(preset.id, it) }
            }
        }
    }

    suspend fun bestTime(difficultyId: String): Int? =
        context.scoreDataStore.data.map { it[key(difficultyId)] }.first()

    /** Records [seconds] if it beats the stored best. Returns true when a new record is set. */
    suspend fun recordTime(difficultyId: String, seconds: Int): Boolean {
        var isRecord = false
        context.scoreDataStore.edit { prefs ->
            val current = prefs[key(difficultyId)]
            if (current == null || seconds < current) {
                prefs[key(difficultyId)] = seconds
                isRecord = true
            }
        }
        return isRecord
    }
}
