package com.example.core.render

import android.graphics.Paint
import android.graphics.RectF

data class RecordedTextBlock(
    val text: String,
    val bounds: RectF,
    val textSizePx: Float,
    val density: Float,
    val isDotText: Boolean,
    val wasTruncated: Boolean
)

object WidgetLayoutRecorder {

    private val activeBlocks = ThreadLocal<MutableList<RecordedTextBlock>?>()
    private val activeDensity = ThreadLocal<Float?>()

    class Capture internal constructor(
        val blocks: List<RecordedTextBlock>,
        private val previous: MutableList<RecordedTextBlock>?,
        private val previousDensity: Float?
    ) : AutoCloseable {
        override fun close() {
            activeBlocks.set(previous)
            activeDensity.set(previousDensity)
        }
    }

    fun begin(density: Float = 1f): Capture {
        val previous = activeBlocks.get()
        val previousDensity = activeDensity.get()
        val blocks = mutableListOf<RecordedTextBlock>()
        activeBlocks.set(blocks)
        activeDensity.set(density)
        return Capture(blocks, previous, previousDensity)
    }

    internal fun recordDotText(
        text: String,
        x: Float,
        y: Float,
        dotGap: Float,
        dotRadius: Float
    ) {
        val blocks = activeBlocks.get() ?: return
        blocks += RecordedTextBlock(
            text = text,
            bounds = RectF(
                x - dotRadius,
                y - dotRadius,
                x + DotMatrixRenderer.measureTextWidth(text.length, dotGap) + dotRadius,
                y + GlyphTable.HEIGHT * dotGap + dotRadius
            ),
            textSizePx = GlyphTable.HEIGHT * dotGap,
            density = activeDensity.get() ?: 1f,
            isDotText = true,
            wasTruncated = false
        )
    }

    internal fun recordLabel(text: String, x: Float, y: Float, paint: Paint, wasTruncated: Boolean) {
        val blocks = activeBlocks.get() ?: return
        val metrics = paint.fontMetrics
        blocks += RecordedTextBlock(
            text = text,
            bounds = RectF(x, y + metrics.ascent, x + paint.measureText(text), y + metrics.descent),
            textSizePx = paint.textSize,
            density = activeDensity.get() ?: 1f,
            isDotText = false,
            wasTruncated = wasTruncated
        )
    }
}
