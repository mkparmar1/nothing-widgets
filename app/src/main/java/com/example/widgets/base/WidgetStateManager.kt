package com.example.widgets.base

import android.content.Context
import android.content.SharedPreferences
import kotlin.random.Random

object WidgetStateManager {

    private const val PREFS_NAME = "dot_widgets_interactive_state"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ==========================================
    // 1. DICE ROLL GAME
    // ==========================================
    fun getDiceRoll(context: Context): Pair<Int, Int> {
        val prefs = getPrefs(context)
        val d1 = prefs.getInt("dice_1", 3)
        val d2 = prefs.getInt("dice_2", 5)
        return d1 to d2
    }

    fun rollDice(context: Context): Pair<Int, Int> {
        val d1 = Random.nextInt(1, 7)
        val d2 = Random.nextInt(1, 7)
        getPrefs(context).edit()
            .putInt("dice_1", d1)
            .putInt("dice_2", d2)
            .apply()
        return d1 to d2
    }

    // ==========================================
    // 2. RETRO SNAKE GAME
    // ==========================================
    data class SnakeState(
        val body: List<Pair<Int, Int>>,
        val food: Pair<Int, Int>,
        val score: Int,
        val direction: String
    )

    fun getSnakeState(context: Context): SnakeState {
        val prefs = getPrefs(context)
        val score = prefs.getInt("snake_score", 0)
        val dir = prefs.getString("snake_dir", "RIGHT") ?: "RIGHT"
        val foodX = prefs.getInt("snake_food_x", 11)
        val foodY = prefs.getInt("snake_food_y", 3)

        val bodyRaw = prefs.getString("snake_body", "3,2;4,2;5,2;6,2;7,2") ?: "3,2;4,2;5,2;6,2;7,2"
        val body = bodyRaw.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                val x = parts[0].toIntOrNull() ?: 0
                val y = parts[1].toIntOrNull() ?: 0
                x to y
            } else null
        }

        return SnakeState(
            body = if (body.isNotEmpty()) body else listOf(3 to 2, 4 to 2, 5 to 2),
            food = foodX to foodY,
            score = score,
            direction = dir
        )
    }

    fun stepSnake(context: Context): SnakeState {
        val current = getSnakeState(context)
        val head = current.body.last()

        val cols = 15
        val rows = 8

        // Move head towards food or in direction
        val dx = when {
            head.first < current.food.first -> 1
            head.first > current.food.first -> -1
            else -> 0
        }
        val dy = when {
            dx == 0 && head.second < current.food.second -> 1
            dx == 0 && head.second > current.food.second -> -1
            else -> 0
        }

        val nextX = ((head.first + (if (dx != 0) dx else 1)) % cols + cols) % cols
        val nextY = ((head.second + dy) % rows + rows) % rows
        val nextHead = nextX to nextY

        val ateFood = nextHead == current.food
        val newScore = if (ateFood) current.score + 10 else current.score
        val newBody = current.body.toMutableList()
        newBody.add(nextHead)
        if (!ateFood && newBody.size > 3) {
            newBody.removeAt(0)
        }

        val newFood = if (ateFood) {
            val fx = Random.nextInt(0, cols)
            val fy = Random.nextInt(0, rows)
            fx to fy
        } else {
            current.food
        }

        val bodyStr = newBody.joinToString(";") { "${it.first},${it.second}" }
        getPrefs(context).edit()
            .putInt("snake_score", newScore)
            .putInt("snake_food_x", newFood.first)
            .putInt("snake_food_y", newFood.second)
            .putString("snake_body", bodyStr)
            .apply()

        return SnakeState(newBody, newFood, newScore, current.direction)
    }

    // ==========================================
    // 3. STOPWATCH / CHRONOMETER
    // ==========================================
    data class StopwatchState(
        val isRunning: Boolean,
        val elapsedMs: Long,
        val lapCount: Int
    )

    fun getStopwatchState(context: Context): StopwatchState {
        val prefs = getPrefs(context)
        val isRunning = prefs.getBoolean("sw_running", false)
        val startTime = prefs.getLong("sw_start_time", 0L)
        val accumulated = prefs.getLong("sw_accumulated", 0L)
        val lapCount = prefs.getInt("sw_lap_count", 1)

        val totalElapsed = if (isRunning && startTime > 0) {
            accumulated + (System.currentTimeMillis() - startTime)
        } else {
            accumulated
        }

        return StopwatchState(isRunning, totalElapsed, lapCount)
    }

    fun toggleStopwatch(context: Context): StopwatchState {
        val prefs = getPrefs(context)
        val isRunning = prefs.getBoolean("sw_running", false)
        val startTime = prefs.getLong("sw_start_time", 0L)
        val accumulated = prefs.getLong("sw_accumulated", 0L)
        val lapCount = prefs.getInt("sw_lap_count", 1)

        val editor = prefs.edit()
        val now = System.currentTimeMillis()

        if (isRunning) {
            // Pause
            val newAcc = accumulated + (now - startTime)
            editor.putBoolean("sw_running", false)
            editor.putLong("sw_accumulated", newAcc)
            editor.putLong("sw_start_time", 0L)
            editor.putInt("sw_lap_count", lapCount + 1)
            editor.apply()
            return StopwatchState(false, newAcc, lapCount + 1)
        } else {
            // Start
            editor.putBoolean("sw_running", true)
            editor.putLong("sw_start_time", now)
            editor.apply()
            return StopwatchState(true, accumulated, lapCount)
        }
    }

    fun resetStopwatch(context: Context) {
        getPrefs(context).edit()
            .putBoolean("sw_running", false)
            .putLong("sw_start_time", 0L)
            .putLong("sw_accumulated", 0L)
            .putInt("sw_lap_count", 1)
            .apply()
    }

    // ==========================================
    // 4. FOCUS / POMODORO TIMER
    // ==========================================
    data class FocusTimerState(
        val isRunning: Boolean,
        val remainingSeconds: Int,
        val totalSeconds: Int
    )

    fun getFocusTimerState(context: Context): FocusTimerState {
        val prefs = getPrefs(context)
        val totalSeconds = prefs.getInt("timer_total_seconds", 25 * 60)
        val isRunning = prefs.getBoolean("timer_running", false)
        val endTime = prefs.getLong("timer_end_time", 0L)
        val remainingPaused = prefs.getInt("timer_remaining_paused", 25 * 60)

        val remaining = if (isRunning) {
            val diffSec = ((endTime - System.currentTimeMillis()) / 1000).toInt()
            diffSec.coerceAtLeast(0)
        } else {
            remainingPaused
        }

        return FocusTimerState(isRunning, remaining, totalSeconds)
    }

    fun toggleFocusTimer(context: Context): FocusTimerState {
        val current = getFocusTimerState(context)
        val prefs = getPrefs(context)
        val editor = prefs.edit()

        if (current.isRunning) {
            // Pause
            editor.putBoolean("timer_running", false)
            editor.putInt("timer_remaining_paused", current.remainingSeconds)
            editor.apply()
            return FocusTimerState(false, current.remainingSeconds, current.totalSeconds)
        } else {
            // Start / Resume
            val resumeSec = if (current.remainingSeconds <= 0) current.totalSeconds else current.remainingSeconds
            val endTime = System.currentTimeMillis() + (resumeSec * 1000L)
            editor.putBoolean("timer_running", true)
            editor.putLong("timer_end_time", endTime)
            editor.apply()
            return FocusTimerState(true, resumeSec, current.totalSeconds)
        }
    }

    // ==========================================
    // 5. QUICK TOGGLES STATE
    // ==========================================
    fun getToggleState(context: Context, name: String, defaultVal: Boolean = false): Boolean {
        return getPrefs(context).getBoolean("toggle_$name", defaultVal)
    }

    fun toggleState(context: Context, name: String): Boolean {
        val cur = getToggleState(context, name)
        val next = !cur
        getPrefs(context).edit().putBoolean("toggle_$name", next).apply()
        return next
    }
}
