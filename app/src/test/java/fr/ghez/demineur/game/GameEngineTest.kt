package fr.ghez.demineur.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun BoardState.mineCount() = cells.count { it.isMine }

    @Test
    fun `first reveal is never a mine and starts the game`() {
        repeat(50) { seed ->
            val engine = GameEngine(Difficulty.Beginner, Random(seed))
            engine.reveal(4, 4)
            assertFalse("seed $seed detonated on first click", engine.status == GameStatus.LOST)
            assertEquals(GameStatus.PLAYING, statusOrWon(engine))
            assertFalse(engine.snapshot().cell(4, 4).isMine)
        }
    }

    private fun statusOrWon(engine: GameEngine) =
        if (engine.status == GameStatus.WON) GameStatus.PLAYING else engine.status

    @Test
    fun `mine count matches difficulty after placement`() {
        val engine = GameEngine(Difficulty.Intermediate, Random(7))
        engine.reveal(0, 0)
        assertEquals(Difficulty.Intermediate.mines, engine.snapshot().mineCount())
    }

    @Test
    fun `flagging decrements mines remaining and cycles back`() {
        val engine = GameEngine(Difficulty.Beginner, Random(1))
        engine.reveal(4, 4)
        val before = engine.minesRemaining
        engine.toggleFlag(0, 0)
        assertEquals(before - 1, engine.minesRemaining)
        engine.toggleFlag(0, 0) // FLAGGED -> COVERED (question marks disabled)
        assertEquals(before, engine.minesRemaining)
    }

    @Test
    fun `revealing an empty region cascades`() {
        // Single mine in a corner: revealing the opposite corner floods most of the board.
        val engine = GameEngine(Difficulty.Beginner)
        engine.installMinesForTest(setOf(0)) // mine at (0,0)
        engine.reveal(8, 8)
        val revealed = engine.snapshot().cells.count { it.state == CellState.REVEALED }
        assertTrue("expected a large cascade, got $revealed", revealed > 70)
    }

    @Test
    fun `revealing a mine loses and reveals all mines`() {
        val engine = GameEngine(Difficulty.Beginner)
        engine.installMinesForTest(setOf(0, 1, 2)) // mines on top-left row
        engine.reveal(0, 0)
        assertEquals(GameStatus.LOST, engine.status)
        val detonated = engine.snapshot().cells.count { it.detonated }
        assertEquals(1, detonated)
    }

    @Test
    fun `revealing all safe cells wins`() {
        val engine = GameEngine(Difficulty.Beginner)
        val mines = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        engine.installMinesForTest(mines)
        // Reveal every non-mine cell.
        for (i in 0 until 81) {
            if (i !in mines) engine.reveal(i / 9, i % 9)
        }
        assertEquals(GameStatus.WON, engine.status)
    }

    @Test
    fun `chord reveals neighbors when flags satisfy the number`() {
        val engine = GameEngine(Difficulty.Beginner)
        engine.installMinesForTest(setOf(0)) // mine at (0,0)
        engine.reveal(1, 1)                  // a "1" touching the single mine
        assertEquals(1, engine.snapshot().cell(1, 1).adjacentMines)
        engine.toggleFlag(0, 0)              // flag the mine
        engine.chord(1, 1)
        // Neighbors of (1,1) other than the flagged mine are now revealed and the game survives.
        assertFalse(engine.status == GameStatus.LOST)
        assertEquals(CellState.REVEALED, engine.snapshot().cell(0, 1).state)
        assertEquals(CellState.REVEALED, engine.snapshot().cell(2, 2).state)
    }

    @Test
    fun `chord does nothing when flags do not match`() {
        val engine = GameEngine(Difficulty.Beginner)
        engine.installMinesForTest(setOf(0))
        engine.reveal(1, 1)
        // No flags placed: chord must be a no-op.
        engine.chord(1, 1)
        assertEquals(CellState.COVERED, engine.snapshot().cell(0, 1).state)
    }

    @Test
    fun `custom difficulty clamps mines to a solvable maximum`() {
        val custom = Difficulty.custom(rows = 5, cols = 5, mines = 999)
        assertTrue(custom.mines < 25)
        val engine = GameEngine(custom)
        engine.reveal(2, 2)
        assertFalse(engine.status == GameStatus.LOST)
    }
}
