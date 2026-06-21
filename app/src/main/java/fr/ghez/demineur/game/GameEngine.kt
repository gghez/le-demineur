package fr.ghez.demineur.game

import kotlin.random.Random

/**
 * Pure, Android-free Minesweeper logic for a single game. The engine owns the mutable board and
 * exposes an immutable [BoardState] via [snapshot]; callers drive it with [reveal], [toggleFlag]
 * and [chord]. Mines are placed lazily on the first reveal so that the first click is always safe.
 */
class GameEngine(
    val difficulty: Difficulty,
    private val random: Random = Random.Default,
    /** When true, long-press cycles COVERED → FLAGGED → QUESTION → COVERED (classic "?" option). */
    private val questionMarksEnabled: Boolean = false,
) {
    val rows: Int = difficulty.rows
    val cols: Int = difficulty.cols
    val mineCount: Int = difficulty.mines.coerceIn(1, Difficulty.maxMines(rows, cols))

    private val mine = Array(rows) { BooleanArray(cols) }
    private val adjacent = Array(rows) { IntArray(cols) }
    private val cellState = Array(rows) { Array(cols) { CellState.COVERED } }

    private var minesPlaced = false
    private var detonatedAt: Pair<Int, Int>? = null
    var status: GameStatus = GameStatus.READY
        private set

    private var flagCount = 0
    private var revealedCount = 0

    val minesRemaining: Int get() = mineCount - flagCount

    private fun inBounds(r: Int, c: Int) = r in 0 until rows && c in 0 until cols

    private inline fun forEachNeighbor(r: Int, c: Int, action: (Int, Int) -> Unit) {
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (inBounds(nr, nc)) action(nr, nc)
        }
    }

    /** Place mines avoiding the first-clicked cell and (board permitting) its neighbors. */
    private fun placeMines(safeRow: Int, safeCol: Int) {
        val safe = HashSet<Int>()
        safe.add(safeRow * cols + safeCol)
        // Reserve the 3x3 pocket around the first click only if enough free cells remain.
        if (rows * cols - mineCount > 9) {
            forEachNeighbor(safeRow, safeCol) { r, c -> safe.add(r * cols + c) }
        }

        val candidates = (0 until rows * cols).filterNot { it in safe }.toMutableList()
        // Partial Fisher–Yates: pick the first [mineCount] of a shuffled candidate list.
        var placed = 0
        while (placed < mineCount && candidates.isNotEmpty()) {
            val idx = random.nextInt(candidates.size - placed)
            val pick = candidates[idx]
            candidates[idx] = candidates[candidates.size - 1 - placed]
            candidates[candidates.size - 1 - placed] = pick
            mine[pick / cols][pick % cols] = true
            placed++
        }

        computeAdjacency()
        minesPlaced = true
    }

    private fun computeAdjacency() {
        for (r in 0 until rows) for (c in 0 until cols) {
            if (mine[r][c]) continue
            var count = 0
            forEachNeighbor(r, c) { nr, nc -> if (mine[nr][nc]) count++ }
            adjacent[r][c] = count
        }
    }

    /**
     * Test seam: install a fixed mine layout (row-major indices) instead of random placement,
     * so flood-fill, chord and win/lose logic can be exercised deterministically.
     */
    internal fun installMinesForTest(positions: Set<Int>) {
        check(!minesPlaced) { "mines already placed" }
        for (p in positions) mine[p / cols][p % cols] = true
        computeAdjacency()
        minesPlaced = true
        status = GameStatus.PLAYING
    }

    /** Reveal a covered cell. No-op if flagged, already revealed, or the game is over. */
    fun reveal(row: Int, col: Int) {
        if (!inBounds(row, col) || status == GameStatus.WON || status == GameStatus.LOST) return
        if (cellState[row][col] != CellState.COVERED && cellState[row][col] != CellState.QUESTION) return

        if (!minesPlaced) {
            placeMines(row, col)
            status = GameStatus.PLAYING
        }

        if (mine[row][col]) {
            detonatedAt = row to col
            loseGame()
            return
        }

        floodReveal(row, col)
        checkWin()
    }

    /** Iterative flood fill: reveals [start] and cascades through zero-adjacency neighbors. */
    private fun floodReveal(startRow: Int, startCol: Int) {
        val stack = ArrayDeque<Int>()
        stack.addLast(startRow * cols + startCol)
        while (stack.isNotEmpty()) {
            val key = stack.removeLast()
            val r = key / cols
            val c = key % cols
            if (cellState[r][c] == CellState.REVEALED) continue
            if (cellState[r][c] == CellState.FLAGGED) continue
            cellState[r][c] = CellState.REVEALED
            revealedCount++
            if (adjacent[r][c] == 0) {
                forEachNeighbor(r, c) { nr, nc ->
                    if (cellState[nr][nc] == CellState.COVERED || cellState[nr][nc] == CellState.QUESTION) {
                        stack.addLast(nr * cols + nc)
                    }
                }
            }
        }
    }

    /** Cycle a marker on a non-revealed cell. */
    fun toggleFlag(row: Int, col: Int) {
        if (!inBounds(row, col) || status == GameStatus.WON || status == GameStatus.LOST) return
        when (cellState[row][col]) {
            CellState.COVERED -> {
                cellState[row][col] = CellState.FLAGGED
                flagCount++
            }
            CellState.FLAGGED -> {
                flagCount--
                cellState[row][col] = if (questionMarksEnabled) CellState.QUESTION else CellState.COVERED
            }
            CellState.QUESTION -> cellState[row][col] = CellState.COVERED
            CellState.REVEALED -> Unit
        }
    }

    /**
     * Chord on a revealed number: if its flagged-neighbor count equals the number, reveal every
     * non-flagged neighbor at once (the classic double-click). A wrong flag here loses the game.
     */
    fun chord(row: Int, col: Int) {
        if (!inBounds(row, col) || status != GameStatus.PLAYING) return
        if (cellState[row][col] != CellState.REVEALED || adjacent[row][col] == 0) return

        var flags = 0
        forEachNeighbor(row, col) { r, c -> if (cellState[r][c] == CellState.FLAGGED) flags++ }
        if (flags != adjacent[row][col]) return

        forEachNeighbor(row, col) { r, c ->
            if (cellState[r][c] == CellState.COVERED || cellState[r][c] == CellState.QUESTION) {
                reveal(r, c)
            }
        }
    }

    private fun loseGame() {
        status = GameStatus.LOST
    }

    private fun checkWin() {
        if (revealedCount == rows * cols - mineCount) {
            status = GameStatus.WON
        }
    }

    fun snapshot(): BoardState {
        val gameOver = status == GameStatus.WON || status == GameStatus.LOST
        val cells = ArrayList<CellView>(rows * cols)
        for (r in 0 until rows) for (c in 0 until cols) {
            val state = cellState[r][c]
            // On a win, surface remaining mines as flags for the classic "all flagged" look.
            val displayState = when {
                status == GameStatus.WON && mine[r][c] && state != CellState.FLAGGED -> CellState.FLAGGED
                status == GameStatus.LOST && mine[r][c] && state == CellState.COVERED -> CellState.REVEALED
                status == GameStatus.LOST && mine[r][c] && state == CellState.QUESTION -> CellState.REVEALED
                else -> state
            }
            cells.add(
                CellView(
                    row = r,
                    col = c,
                    state = displayState,
                    isMine = mine[r][c],
                    adjacentMines = adjacent[r][c],
                    detonated = gameOver && detonatedAt == (r to c),
                    wrongFlag = status == GameStatus.LOST && state == CellState.FLAGGED && !mine[r][c],
                ),
            )
        }
        return BoardState(rows, cols, cells, status, minesRemaining)
    }
}
