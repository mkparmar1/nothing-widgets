package com.example.core.astro

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Offline moon-phase and sunrise/sunset calculations (accurate to a few minutes). */
object Astronomy {

    private const val SYNODIC_MONTH = 29.530588853
    // A known new moon: 2000-01-06 18:14 UTC
    private const val KNOWN_NEW_MOON_MS = 947182440000L

    data class MoonPhase(
        /** 0 = new, 0.5 = full, back to 1 = new. */
        val fraction: Double,
        /** 0..1 portion of the disc that is lit. */
        val illumination: Double,
        val name: String,
        val daysToFull: Int,
        val daysToNew: Int
    ) {
        val isWaxing: Boolean get() = fraction < 0.5
    }

    fun moonPhase(timeMs: Long = System.currentTimeMillis()): MoonPhase {
        val days = (timeMs - KNOWN_NEW_MOON_MS) / 86_400_000.0
        val age = ((days % SYNODIC_MONTH) + SYNODIC_MONTH) % SYNODIC_MONTH
        val fraction = age / SYNODIC_MONTH
        val illumination = (1 - cos(2 * PI * fraction)) / 2
        val name = when {
            age < 1.0 || age > SYNODIC_MONTH - 1.0 -> "NEW MOON"
            age < 6.4 -> "WAXING CRESCENT"
            age < 8.4 -> "FIRST QUARTER"
            age < 13.8 -> "WAXING GIBBOUS"
            age < 15.8 -> "FULL MOON"
            age < 21.1 -> "WANING GIBBOUS"
            age < 23.1 -> "LAST QUARTER"
            else -> "WANING CRESCENT"
        }
        val half = SYNODIC_MONTH / 2
        val toFull = if (age <= half) half - age else SYNODIC_MONTH - age + half
        val toNew = SYNODIC_MONTH - age
        return MoonPhase(fraction, illumination, name, toFull.toInt(), toNew.toInt())
    }

    data class SunTimes(
        /** Minutes after local midnight, or null when the sun doesn't rise/set (polar day/night). */
        val sunriseMin: Int?,
        val sunsetMin: Int?
    )

    /** NOAA sunrise equation for the given day, location and time zone. */
    fun sunTimes(latitude: Double, longitude: Double, timeZone: TimeZone, cal: Calendar = Calendar.getInstance(timeZone)): SunTimes {
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val gamma = 2 * PI / 365 * (dayOfYear - 1)
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
            0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

        val latRad = Math.toRadians(latitude)
        val cosHa = cos(Math.toRadians(90.833)) / (cos(latRad) * cos(decl)) - tan(latRad) * tan(decl)
        if (cosHa < -1 || cosHa > 1) return SunTimes(null, null)
        val ha = Math.toDegrees(acos(cosHa))

        val offsetMin = timeZone.getOffset(cal.timeInMillis) / 60_000.0
        val solarNoon = 720 - 4 * longitude - eqTime + offsetMin
        return SunTimes(
            sunriseMin = floor(solarNoon - 4 * ha).toInt().mod(1440),
            sunsetMin = floor(solarNoon + 4 * ha).toInt().mod(1440)
        )
    }
}
