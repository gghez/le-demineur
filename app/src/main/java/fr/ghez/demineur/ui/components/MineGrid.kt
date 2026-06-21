package fr.ghez.demineur.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import fr.ghez.demineur.game.BoardState
import fr.ghez.demineur.game.CellState
import fr.ghez.demineur.game.CellView
import fr.ghez.demineur.ui.theme.Win95

/** Edge length of one cell. Sized for comfortable touch; the board scrolls when larger than screen. */
val CELL_SIZE = 26.dp

@Composable
fun MineGrid(
    board: BoardState,
    onCellTap: (row: Int, col: Int) -> Unit,
    onCellLongPress: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .size(width = CELL_SIZE * board.cols, height = CELL_SIZE * board.rows)
            .pointerInput(board.rows, board.cols) {
                val cs = size.width.toFloat() / board.cols
                detectTapGestures(
                    onTap = { pos -> dispatch(pos, cs, board, onCellTap) },
                    onLongPress = { pos -> dispatch(pos, cs, board, onCellLongPress) },
                )
            },
    ) {
        val cs = size.width / board.cols
        for (cell in board.cells) {
            drawCell(cell, cs, measurer)
        }
    }
}

private fun dispatch(
    pos: Offset,
    cellSize: Float,
    board: BoardState,
    action: (Int, Int) -> Unit,
) {
    val col = (pos.x / cellSize).toInt().coerceIn(0, board.cols - 1)
    val row = (pos.y / cellSize).toInt().coerceIn(0, board.rows - 1)
    action(row, col)
}

private fun DrawScope.drawCell(cell: CellView, cs: Float, measurer: TextMeasurer) {
    val left = cell.col * cs
    val top = cell.row * cs
    val origin = Offset(left, top)

    val opened = cell.state == CellState.REVEALED
    if (opened || cell.wrongFlag) {
        drawOpenedTile(cell, origin, cs, measurer)
    } else {
        drawCoveredTile(cell, origin, cs)
    }
}

/** A flat revealed cell with thin grid lines, possibly showing a number or a mine. */
private fun DrawScope.drawOpenedTile(cell: CellView, origin: Offset, cs: Float, measurer: TextMeasurer) {
    val detonated = cell.detonated
    drawRect(if (detonated) Color(0xFFFF0000) else Win95.Face, origin, Size(cs, cs))
    // grid lines on top + left
    drawRect(Win95.Shadow, origin, Size(cs, 1f))
    drawRect(Win95.Shadow, origin, Size(1f, cs))

    when {
        cell.wrongFlag -> {
            drawMine(origin, cs)
            drawCrossOut(origin, cs)
        }
        cell.isMine -> drawMine(origin, cs)
        cell.adjacentMines > 0 -> drawNumber(cell.adjacentMines, origin, cs, measurer)
    }
}

/** A raised covered cell, optionally flagged or question-marked. */
private fun DrawScope.drawCoveredTile(cell: CellView, origin: Offset, cs: Float) {
    drawRect(Win95.Face, origin, Size(cs, cs))
    val t = 2f
    // highlight top + left
    drawRect(Win95.Highlight, origin, Size(cs, t))
    drawRect(Win95.Highlight, origin, Size(t, cs))
    // shadow bottom + right
    drawRect(Win95.Shadow, Offset(origin.x, origin.y + cs - t), Size(cs, t))
    drawRect(Win95.Shadow, Offset(origin.x + cs - t, origin.y), Size(t, cs))

    when (cell.state) {
        CellState.FLAGGED -> drawFlag(origin, cs)
        CellState.QUESTION -> drawQuestion(origin, cs)
        else -> Unit
    }
}

private fun DrawScope.drawNumber(n: Int, origin: Offset, cs: Float, measurer: TextMeasurer) {
    val layout = measurer.measure(
        text = n.toString(),
        style = TextStyle(
            color = Win95.numberColors[n],
            fontSize = (cs * 0.62f).toSp(),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        ),
    )
    drawText(
        layout,
        topLeft = Offset(
            origin.x + (cs - layout.size.width) / 2f,
            origin.y + (cs - layout.size.height) / 2f,
        ),
    )
}

private fun DrawScope.drawMine(origin: Offset, cs: Float) {
    val cx = origin.x + cs / 2
    val cy = origin.y + cs / 2
    val r = cs * 0.22f
    val spike = cs * 0.34f
    val s = 2f
    // spikes
    drawLine(Color.Black, Offset(cx, cy - spike), Offset(cx, cy + spike), s)
    drawLine(Color.Black, Offset(cx - spike, cy), Offset(cx + spike, cy), s)
    val d = spike * 0.72f
    drawLine(Color.Black, Offset(cx - d, cy - d), Offset(cx + d, cy + d), s)
    drawLine(Color.Black, Offset(cx + d, cy - d), Offset(cx - d, cy + d), s)
    // body + glint
    drawCircle(Color.Black, r, Offset(cx, cy))
    drawRect(Color.White, Offset(cx - r * 0.5f, cy - r * 0.5f), Size(r * 0.35f, r * 0.35f))
}

private fun DrawScope.drawCrossOut(origin: Offset, cs: Float) {
    val pad = cs * 0.12f
    drawLine(
        Color.Red,
        Offset(origin.x + pad, origin.y + pad),
        Offset(origin.x + cs - pad, origin.y + cs - pad),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawFlag(origin: Offset, cs: Float) {
    val poleX = origin.x + cs * 0.46f
    val topY = origin.y + cs * 0.22f
    // flag cloth
    val flag = Path().apply {
        moveTo(poleX, topY)
        lineTo(poleX, topY + cs * 0.22f)
        lineTo(origin.x + cs * 0.24f, topY + cs * 0.11f)
        close()
    }
    drawPath(flag, Color(0xFFFF0000))
    // pole
    drawLine(
        Color.Black,
        Offset(poleX, topY),
        Offset(poleX, origin.y + cs * 0.70f),
        strokeWidth = 2f,
    )
    // base
    drawRect(Color.Black, Offset(origin.x + cs * 0.28f, origin.y + cs * 0.70f), Size(cs * 0.40f, cs * 0.08f))
}

private fun DrawScope.drawQuestion(origin: Offset, cs: Float) {
    // Approximate a "?" with an arc and a dot.
    drawArc(
        color = Color.Black,
        startAngle = 160f,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = Offset(origin.x + cs * 0.30f, origin.y + cs * 0.20f),
        size = Size(cs * 0.40f, cs * 0.36f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round),
    )
    drawLine(
        Color.Black,
        Offset(origin.x + cs * 0.50f, origin.y + cs * 0.45f),
        Offset(origin.x + cs * 0.50f, origin.y + cs * 0.58f),
        strokeWidth = 2.5f,
    )
    drawCircle(Color.Black, cs * 0.04f, Offset(origin.x + cs * 0.50f, origin.y + cs * 0.70f))
}
