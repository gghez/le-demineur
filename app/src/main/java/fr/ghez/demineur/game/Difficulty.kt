package fr.ghez.demineur.game

/**
 * A board configuration. The three classic Windows presets are singletons; [Custom] lets the
 * player pick arbitrary dimensions. [id] is the stable key used for local high-scores and to
 * map a difficulty onto a Play Games leaderboard.
 */
sealed interface Difficulty {
    val rows: Int
    val cols: Int
    val mines: Int
    val id: String

    /** Whether scores for this difficulty are eligible for the global (Play Games) leaderboard. */
    val isRanked: Boolean get() = this !is Custom

    data object Beginner : Difficulty {
        override val rows = 9
        override val cols = 9
        override val mines = 10
        override val id = "beginner"
    }

    data object Intermediate : Difficulty {
        override val rows = 16
        override val cols = 16
        override val mines = 40
        override val id = "intermediate"
    }

    data object Expert : Difficulty {
        override val rows = 16
        override val cols = 30
        override val mines = 99
        override val id = "expert"
    }

    data class Custom(
        override val rows: Int,
        override val cols: Int,
        override val mines: Int,
    ) : Difficulty {
        override val id = "custom"
    }

    companion object {
        /** Bounds enforced for custom boards, mirroring the original game's accepted ranges. */
        const val MIN_DIMENSION = 5
        const val MAX_ROWS = 24
        const val MAX_COLS = 30

        val presets: List<Difficulty> = listOf(Beginner, Intermediate, Expert)

        /** Largest mine count that still leaves room for a safe first-click pocket. */
        fun maxMines(rows: Int, cols: Int): Int = (rows * cols - 1).coerceAtLeast(1)

        fun custom(rows: Int, cols: Int, mines: Int): Custom {
            val r = rows.coerceIn(MIN_DIMENSION, MAX_ROWS)
            val c = cols.coerceIn(MIN_DIMENSION, MAX_COLS)
            val m = mines.coerceIn(1, maxMines(r, c))
            return Custom(r, c, m)
        }
    }
}
