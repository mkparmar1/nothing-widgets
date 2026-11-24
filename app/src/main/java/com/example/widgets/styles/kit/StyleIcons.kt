package com.example.widgets.styles.kit

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import java.util.concurrent.ConcurrentHashMap

/**
 * Draws Material Symbols (the app already ships material-icons-extended, Apache-2.0) onto a widget
 * bitmap, so the new styles get crisp solid glyph icons like the mockups without bundling images.
 */
object StyleIcons {

    private class Glyph(val paths: List<Path>, val viewport: Float)

    private val cache = ConcurrentHashMap<ImageVector, Glyph>()

    private fun glyph(vector: ImageVector): Glyph = cache.getOrPut(vector) {
        val out = mutableListOf<Path>()
        fun walk(group: VectorGroup) {
            for (node in group) {
                when (node) {
                    is VectorPath -> {
                        val path = PathParser().addPathNodes(node.pathData).toPath().asAndroidPath()
                        path.fillType = if (node.pathFillType == PathFillType.EvenOdd) Path.FillType.EVEN_ODD else Path.FillType.WINDING
                        out += path
                    }
                    is VectorGroup -> walk(node)
                }
            }
        }
        walk(vector.root)
        Glyph(out, maxOf(vector.viewportWidth, vector.viewportHeight))
    }

    /** Draws [vector] centred on ([cx], [cy]) inside a [size]-pixel square, filled with [color]. */
    fun draw(canvas: Canvas, vector: ImageVector, cx: Float, cy: Float, size: Float, color: Int) {
        if (size <= 0f) return
        val g = glyph(vector)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
        canvas.save()
        canvas.translate(cx - size / 2f, cy - size / 2f)
        val scale = size / g.viewport
        canvas.scale(scale, scale)
        g.paths.forEach { canvas.drawPath(it, paint) }
        canvas.restore()
    }
}
