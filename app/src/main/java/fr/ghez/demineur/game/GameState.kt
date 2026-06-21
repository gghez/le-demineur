package fr.ghez.demineur.game

/** Lifecycle of a single game. */
enum class GameStatus {
    /** Board generated but no cell revealed yet; the timer has not started. */
    READY,
    PLAYING,
    WON,
    LOST,
}

/** Visible state of one cell, as consumed by the UI. */
enum class CellState {
    COVERED,
    REVEALED,
    FLAGGED,
    QUESTION,
}

/**
 * Immutable view of a single cell. [adjacentMines] and [isMine] are only meaningful for the UI
 * once the cell is [CellState.REVEALED] or the game is over.
 */
data class CellView(
    val row: Int,
    val col: Int,
    val state: CellState,
    val isMine: Boolean,
    val adjacentMines: Int,
    /** True for the mine the player detonated, so the UI can paint it red. */
    val detonated: Boolean = false,
    /** True for a flag that turned out to be wrong, shown crossed-out on loss. */
    val wrongFlag: Boolean = false,
)

/**
 * Immutable snapshot of the whole board. [cells] is row-major (`row * cols + col`).
 * Elapsed time is tracked separately by the ViewModel, not by the engine.
 */
data class BoardState(
    val rows: Int,
    val cols: Int,
    val cells: List<CellView>,
    val status: GameStatus,
    val minesRemaining: Int,
) {
    fun cell(row: Int, col: Int): CellView = cells[row * cols + col]
}
