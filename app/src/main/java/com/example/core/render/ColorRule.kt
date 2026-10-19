package com.example.core.render

/**
 * Shared conditional-colour logic so every goal/threshold widget (battery, steps, budget, hydration…)
 * uses identical semantics: the primary value paints in the live accent, and once it crosses
 * [warnRatio] of its goal it switches to a warning tint.
 *
 * Keeps "accent only for live state" honest and means an option can turn the rule off uniformly.
 */
object ColorRule {

    /** Amber-ish warning used by every widget when a threshold is crossed. */
    const val WARN = 0xFFF2A516.toInt()

    /**
     * Picks the paint colour for [value] against [goal].
     *
     * @param value current value (e.g. 0..100 battery, steps taken)
     * @param goal the target; <= 0 means "no goal", so only [accentColor] is used
     * @param warnRatio fraction 0..1 of the goal at which warning kicks in (e.g. 0.2 for "under 20%")
     * @param accentColor the live accent
     * @param belowGoal when true (default) the warning fires *below* the ratio (battery low, budget
     *   low); set false for "warning when exceeding" (spend over budget)
     */
    fun paint(
        value: Number,
        goal: Number,
        warnRatio: Float,
        accentColor: Int,
        enabled: Boolean = true,
        belowGoal: Boolean = true
    ): Int {
        if (!enabled || goal.toFloat() <= 0f || warnRatio <= 0f) return accentColor
        val ratio = value.toFloat() / goal.toFloat()
        val crossed = if (belowGoal) ratio <= warnRatio else ratio >= warnRatio
        return if (crossed) WARN else accentColor
    }
}
