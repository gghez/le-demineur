package fr.ghez.demineur.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.ghez.demineur.data.ScoreRepository
import fr.ghez.demineur.game.BoardState
import fr.ghez.demineur.game.Difficulty
import fr.ghez.demineur.game.GameEngine
import fr.ghez.demineur.game.GameStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI-facing snapshot combining the board with externally-tracked time and best score. */
data class GameUiState(
    val difficulty: Difficulty,
    val board: BoardState,
    val elapsedSeconds: Int,
    val bestSeconds: Int?,
) {
    val status: GameStatus get() = board.status
    val minesRemaining: Int get() = board.minesRemaining
}

/** One-shot events the UI (Activity) must react to with platform APIs. */
sealed interface GameEvent {
    data class Won(val difficulty: Difficulty, val seconds: Int, val isNewRecord: Boolean) : GameEvent
}

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val scoreRepo = ScoreRepository(app)

    private var difficulty: Difficulty = Difficulty.Beginner
    private var engine = GameEngine(difficulty)
    private var elapsed = 0
    private var bestSeconds: Int? = null
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    init {
        loadBest()
    }

    private fun buildState() = GameUiState(
        difficulty = difficulty,
        board = engine.snapshot(),
        elapsedSeconds = elapsed,
        bestSeconds = bestSeconds,
    )

    private fun publish() {
        _uiState.value = buildState()
    }

    private fun loadBest() {
        viewModelScope.launch {
            bestSeconds = scoreRepo.bestTime(difficulty.id)
            publish()
        }
    }

    /** Primary tap: chord on a satisfied number, otherwise reveal. */
    fun onCellTap(row: Int, col: Int) {
        if (engine.status == GameStatus.WON || engine.status == GameStatus.LOST) return
        val cell = engine.snapshot().cell(row, col)
        if (cell.state == fr.ghez.demineur.game.CellState.REVEALED && cell.adjacentMines > 0) {
            engine.chord(row, col)
        } else {
            engine.reveal(row, col)
        }
        afterMove()
    }

    fun onCellLongPress(row: Int, col: Int) {
        if (engine.status == GameStatus.WON || engine.status == GameStatus.LOST) return
        engine.toggleFlag(row, col)
        afterMove()
    }

    fun onSmileyTap() = newGame(difficulty)

    fun newGame(newDifficulty: Difficulty) {
        difficulty = newDifficulty
        engine = GameEngine(difficulty)
        elapsed = 0
        stopTimer()
        publish()
        loadBest()
    }

    private fun afterMove() {
        when (engine.status) {
            GameStatus.PLAYING -> startTimerIfNeeded()
            GameStatus.WON -> {
                stopTimer()
                handleWin()
            }
            GameStatus.LOST -> stopTimer()
            GameStatus.READY -> Unit
        }
        publish()
    }

    private fun handleWin() {
        val finalTime = elapsed
        viewModelScope.launch {
            val isRecord = if (difficulty.isRanked) {
                scoreRepo.recordTime(difficulty.id, finalTime)
            } else false
            if (isRecord) {
                bestSeconds = finalTime
                publish()
            }
            _events.emit(GameEvent.Won(difficulty, finalTime, isRecord))
        }
    }

    private fun startTimerIfNeeded() {
        if (timerJob != null) return
        timerJob = viewModelScope.launch {
            while (elapsed < MAX_TIME) {
                delay(1000)
                if (engine.status != GameStatus.PLAYING) break
                elapsed++
                publish()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private companion object {
        const val MAX_TIME = 999
    }
}
