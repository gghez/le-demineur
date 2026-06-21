package fr.ghez.demineur

import android.app.Application
import android.util.Log
import com.google.android.gms.games.PlayGamesSdk

class DemineurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Guarded: a placeholder/missing APP_ID must never crash the app.
        runCatching { PlayGamesSdk.initialize(this) }
            .onFailure { Log.w("DemineurApp", "PlayGamesSdk init skipped", it) }
    }
}
