package com.example.widgets.catalogue

import android.content.Context
import android.graphics.Canvas
import com.example.core.bitmap.SizeBucket
import com.example.core.render.ColorRule
import com.example.core.render.DotMatrixRenderer
import com.example.core.render.RenderKit
import com.example.data.finance.FinanceRepository
import com.example.data.finance.FxRepository
import com.example.data.finance.StockRepository
import com.example.data.market.CryptoRepository
import com.example.widgets.base.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Phase 7 Finance family. Every renderer is offline-first: it reads a cache or the local
 * [FinanceRepository] and degrades to a clean empty state rather than blocking on the network.
 */
object FinanceRenderers {

    // ---------------------------------------------------------------- crypto ticker

    fun renderCryptoTick(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val quote = CryptoRepository.quote(context)
        val coin = spec.options.string("coin", "BTC")
        if (quote == null) {
            kit.header("$coin / ${kit.spec.options.string("quote", "USD")}")
            kit.labelCentered("LOADING PRICE…", kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }
        kit.header("$coin / ${quote.currency}")
        drawPriceBig(kit, formatPrice(quote.price, quote.currency))

        val up = quote.change24hPct >= 0
        // "accent only for live state": colour the change only when the up/down rule is enabled.
        val ruleOn = spec.options.boolean("up_down_colour", true)
        val changeColor = when {
            !ruleOn -> kit.onColor
            up -> kit.onColor
            else -> kit.accentColor
        }
        drawChangeTag(kit, quote.change24hPct, changeColor)
    }

    fun renderCryptoSpark(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val quote = CryptoRepository.quote(context)
        val coin = spec.options.string("coin", "BTC")
        if (quote == null) {
            kit.header("$coin TREND")
            kit.labelCentered("LOADING…", kit.height * 0.55f, kit.textPaint(kit.onColor, 11f, 15f))
            return
        }
        kit.header("$coin • ${spec.options.string("range", "24H")}")
        drawPriceBig(kit, formatPrice(quote.price, quote.currency))
        drawSparkline(kit, quote.history, fill = spec.options.boolean("fill", true))
    }

    // ---------------------------------------------------------------- stocks

    fun renderStockTick(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val symbol = spec.options.string("symbol", "AAPL")
        kit.header("$symbol • ${spec.options.string("market", "US")}")
        val quote = StockRepository.quote(context, symbol)
        if (quote == null) {
            kit.labelCentered("NO DATA YET", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 15f))
            return
        }
        drawPriceBig(kit, String.format(Locale.US, "%.2f", quote.price))
        drawChangeTag(kit, quote.changePct, kit.accentColor)
    }

    fun renderStockSpark(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val symbol = spec.options.string("symbol", "AAPL")
        kit.header("$symbol • ${spec.options.string("range", "1M")}")
        val quote = StockRepository.quote(context, symbol)
        if (quote == null || quote.history.size < 2) {
            kit.labelCentered("NO DATA YET", kit.height * 0.55f, kit.textPaint(kit.greyColor, 11f, 15f))
            return
        }
        drawPriceBig(kit, String.format(Locale.US, "%.2f", quote.price))
        drawSparkline(kit, quote.history, fill = true)
    }

    // ---------------------------------------------------------------- FX

    fun renderCurrencyPair(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        FxRepository.refresh(context)
        val base = spec.options.string("base", "USD")
        val quote = spec.options.string("quote", "INR")
        val inverse = spec.options.boolean("inverse", false)
        kit.header("$base → $quote")
        val rate = if (inverse) FxRepository.convert(1.0, quote, base) else FxRepository.convert(1.0, base, quote)
        drawPriceBig(kit, String.format(Locale.US, "%.3f", rate))
        val live = FxRepository.fetchedAt() > 0L
        kit.labelCentered(
            if (live) "LIVE RATE" else "OFFLINE RATE",
            kit.height - kit.pad - 4f * kit.d, kit.textPaint(kit.greyColor, 9f, 12f)
        )
    }

    fun renderCurrencyMulti(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        FxRepository.refresh(context)
        val base = spec.options.string("base", "USD")
        val pairs = spec.options.string("pairs", "EUR,GBP,JPY,INR,AUD,CAD").split(",")
            .map { it.trim().uppercase() }.filter { it.isNotBlank() }.take(6)
        kit.header("FX BOARD • $base")
        val cols = if (spec.sizeBucket == SizeBucket.L) 3 else 2
        val rows = ((pairs.size + cols - 1) / cols).coerceAtLeast(1)
        val top = kit.pad + 22f * kit.d
        val cellW = kit.maxW / cols
        val cellH = (kit.height - top - kit.pad) / rows
        pairs.forEachIndexed { i, cur ->
            val cx = kit.pad + (i % cols) * cellW
            val cy = top + (i / cols) * cellH
            val paintCode = kit.textPaint(kit.greyColor, 9f, 12f)
            canvas.drawText(cur, cx, cy + 10f * kit.d, paintCode)
            val rate = FxRepository.convert(1.0, base, cur)
            val paintVal = kit.textPaint(kit.onColor, 12f, 16f)
            canvas.drawText(String.format(Locale.US, "%.2f", rate), cx, cy + cellH - 4f * kit.d, paintVal)
        }
    }

    // ---------------------------------------------------------------- market status

    fun renderMarketStatus(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val exchange = spec.options.string("exchange", "NYSE")
        val now = Calendar.getInstance()
        val day = now.get(Calendar.DAY_OF_WEEK)
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val isWeekend = day == Calendar.SATURDAY || day == Calendar.SUNDAY
        val open = !isWeekend && hour in 9..15
        kit.header("$exchange")
        val label = when {
            isWeekend -> "CLOSED · WEEKEND"
            open -> "MARKET OPEN"
            else -> "MARKET CLOSED"
        }
        val color = ColorRule.paint(if (open) 1 else 0, 1, 0.5f, kit.accentColor, enabled = open)
        kit.labelCentered(label, kit.height * 0.56f, kit.textPaint(if (open) color else kit.greyColor, 12f, 16f))
    }

    // ---------------------------------------------------------------- budget / spend

    fun renderSpendDaily(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = FinanceRepository.getInstance(context)
        val budget = spec.options.int("budget", repo.getDailyBudget()).coerceAtLeast(1)
        val spent = repo.getSpentToday()
        val symbol = repo.getBudgetCurrency()
        kit.header("TODAY'S SPEND")
        drawPriceBig(kit, "$symbol$spent")
        kit.labelCentered("OF $symbol$budget", kit.height * 0.68f, kit.textPaint(kit.greyColor, 10f, 13f))
        val budgetColor = ColorRule.paint(spent, budget, 0.9f, kit.accentColor, belowGoal = false)
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = spent, max = budget, totalDots = 20,
            startX = kit.pad, startY = kit.height - kit.pad - 8f * kit.d,
            dotRadius = 2.6f * kit.d, dotGap = kit.maxW / 19f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = budgetColor, useAccentOnActive = true
        )
    }

    fun renderBudgetRing(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = FinanceRepository.getInstance(context)
        val budget = spec.options.int("budget", repo.getDailyBudget()).coerceAtLeast(1)
        val spent = repo.getSpentToday()
        val warnRatio = spec.options.int("warn_pct", 80) / 100f
        kit.header(spec.options.string("period", "DAILY").uppercase() + " BUDGET")

        val cx = kit.width / 2f
        val cy = kit.height * 0.56f
        val radius = minOf(kit.width, kit.height) * 0.24f
        val filled = (spent.toFloat() / budget * 24f).toInt().coerceIn(0, 24)
        val ringColor = ColorRule.paint(spent, budget, warnRatio, kit.accentColor, belowGoal = false)
        DotMatrixRenderer.drawDotRing(
            canvas = canvas, centerX = cx, centerY = cy, radius = radius, totalDots = 24,
            filledDots = filled, dotRadius = radius * 0.14f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = ringColor
        )
        val pct = (spent.toFloat() / budget * 100).toInt()
        val paintPct = kit.textPaint(ringColor, 12f, 16f)
        val str = "$pct%"
        canvas.drawText(str, cx - paintPct.measureText(str) / 2f, cy + paintPct.textSize / 3f, paintPct)
    }

    fun renderSavingsGoal(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = FinanceRepository.getInstance(context)
        val goal = spec.options.int("goal", repo.getSavingsGoal()).coerceAtLeast(1)
        val current = repo.getSavingsCurrent()
        val symbol = repo.getBudgetCurrency()
        kit.header("SAVINGS GOAL")
        val pct = (current.toFloat() / goal * 100).toInt().coerceIn(0, 999)
        kit.dotTextCentered("$pct%", kit.height * 0.52f, kit.height * 0.34f, 10f * kit.d, accentIndices = setOf(3))
        kit.labelCentered("$symbol$current / $symbol$goal", kit.height - kit.pad - 4f * kit.d, kit.textPaint(kit.greyColor, 10f, 13f))
        DotMatrixRenderer.drawDotRun(
            canvas = canvas, current = current, max = goal, totalDots = 24,
            startX = kit.pad, startY = kit.pad + 18f * kit.d - 14f * kit.d,
            dotRadius = 2.2f * kit.d, dotGap = kit.maxW / 23f,
            onColor = kit.onColor, offColor = kit.offColor, accentColor = kit.accentColor, useAccentOnActive = true
        )
    }

    fun renderPaydayCountdown(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = FinanceRepository.getInstance(context)
        // The option overrides the stored payday day-of-month without mutating it.
        if (spec.options.int("payday_dom", 0) > 0) repo.setPaydayDayOfMonth(spec.options.int("payday_dom", 1))
        val days = repo.daysUntilPayday()
        kit.header("PAYDAY")
        kit.dotTextCentered("$days", kit.height * 0.50f, kit.height * 0.42f, 12f * kit.d,
            accentIndices = if (days <= 1) setOf(0) else emptySet())
        val showMonth = spec.options.boolean("show_month", false)
        val sub = when {
            days == 0 -> "TODAY"
            showMonth -> SimpleDateFormat("MMM", Locale.US).format(Calendar.getInstance().time).uppercase() + " PAYDAY"
            else -> "DAY${if (days == 1) "" else "S"} TO GO"
        }
        kit.labelCentered(sub, kit.height - kit.pad - 2f * kit.d, kit.textPaint(kit.accentColor, 10f, 13f))
    }

    fun renderPortfolio(context: Context, spec: WidgetSpec, canvas: Canvas) {
        val kit = RenderKit(spec, canvas)
        kit.background()
        val repo = FinanceRepository.getInstance(context)
        val holdings = repo.getHoldings()
        kit.header("PORTFOLIO • ${holdings.size} ASSETS")
        val quote = CryptoRepository.quote(context)
        // Value the BTC portion against the live (or last) quote; other rows show units.
        var value = 0.0
        val btc = holdings.find { it.symbol == "BTC" }
        if (btc != null && quote != null) value = btc.units * quote.price
        val top = kit.pad + 24f * kit.d
        if (holdings.isEmpty()) {
            kit.labelCentered("ADD HOLDINGS IN THE APP", kit.height * 0.55f, kit.textPaint(kit.greyColor, 10f, 13f))
            return
        }
        val rowH = (kit.height - top - kit.pad) / holdings.size
        holdings.take(6).forEachIndexed { i, h ->
            val y = top + i * rowH + rowH * 0.7f
            val paintSym = kit.textPaint(kit.onColor, 11f, 15f)
            canvas.drawText(h.symbol, kit.pad, y, paintSym)
            val paintUnits = kit.textPaint(kit.greyColor, 10f, 13f)
            val units = String.format(Locale.US, "%.3f", h.units)
            canvas.drawText(units, kit.width - kit.pad - paintUnits.measureText(units), y, paintUnits)
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun formatPrice(price: Double, currency: String): String =
        if (currency.equals("INR", true)) String.format(Locale.US, "%.0f", price)
        else String.format(Locale.US, "%.2f", price)

    private fun drawPriceBig(kit: RenderKit, value: String) {
        val gap = DotMatrixRenderer.fitDotGap(value.length, kit.maxW * 0.72f, kit.height * 0.34f, 11f * kit.d)
        DotMatrixRenderer.drawText(
            canvas = kit.canvas, text = value, startX = kit.pad, startY = kit.height * 0.30f,
            dotRadius = gap * 0.38f, dotGap = gap, onColor = kit.onColor, offColor = kit.offColor
        )
    }

    private fun drawChangeTag(kit: RenderKit, pct: Double, color: Int) {
        val up = pct >= 0
        val text = String.format(Locale.US, "%s%.2f%%", if (up) "+" else "-", abs(pct))
        val paint = kit.textPaint(color, 11f, 15f)
        kit.canvas.drawText(text, kit.pad, kit.height * 0.30f + kit.height * 0.34f + 4f * kit.d, paint)
    }

    private fun drawSparkline(kit: RenderKit, history: List<Double>, fill: Boolean) {
        if (history.size < 2) return
        val min = history.min()
        val range = (history.max() - min).coerceAtLeast(1e-9)
        val top = kit.height * 0.60f
        val h = kit.height - kit.pad - top
        val step = kit.maxW / (history.size - 1)
        history.forEachIndexed { i, p ->
            val y = top + h - ((p - min) / range * h).toFloat()
            val last = i == history.lastIndex
            val r = if (last) 3.4f * kit.d else 2.0f * kit.d
            if (fill && i > 0) {
                val prev = history[i - 1]
                val py = top + h - ((prev - min) / range * h).toFloat()
                kit.canvas.drawLine(kit.pad + (i - 1) * step, py, kit.pad + i * step, y, kit.fill(kit.offColor).apply { strokeWidth = 1.4f * kit.d })
            }
            kit.canvas.drawCircle(kit.pad + i * step, y, r, kit.fill(if (last) kit.accentColor else kit.onColor))
        }
    }
}
