package com.example.data.sports

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A followed team plus its last-known fixtures/score, as cached for the Sports widgets. */
data class TeamSnapshot(
    val team: String,
    val opponent: String,
    val league: String,
    val homeScore: Int,
    val awayScore: Int,
    val live: Boolean,
    /** Next/last fixture kickoff, epoch millis (0 when unknown). */
    val kickoffMs: Long,
    /** Recent results newest-first: 'W', 'D' or 'L'. */
    val form: List<Char>
)

/** One row of the mini league table. */
data class TableRow(val team: String, val points: Int, val played: Int)

/**
 * Sports data for the Phase 7 Sports family. There is no free, key-less, always-on live-score API
 * we can depend on, so this repository is deliberately **honest**: it renders the last-known
 * snapshot the user's configured feed returned, and a bundled F1 calendar that works offline.
 * Without configuration the widgets show a clean "set up in the app" / "no match today" state.
 */
object SportsRepository {

    private const val PREFS = "dot_widgets_sports"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Optional user-provided JSON feed URL (returns the [TeamSnapshot] shape). Empty = not set. */
    fun feedUrl(context: Context): String = prefs(context).getString("feed_url", "") ?: ""

    fun setFeedUrl(context: Context, url: String) =
        prefs(context).edit().putString("feed_url", url.trim()).apply()

    fun isConfigured(context: Context): Boolean = feedUrl(context).isNotBlank()

    /** Last-known snapshot for the followed team, or null until the user sets one up. */
    fun snapshot(context: Context): TeamSnapshot? = try {
        val raw = prefs(context).getString("snapshot", null) ?: return null
        parseSnapshot(JSONObject(raw))
    } catch (_: Exception) {
        null
    }

    /** Stores a snapshot (used by the settings screen and by tests). */
    fun setSnapshot(context: Context, snap: TeamSnapshot) {
        prefs(context).edit().putString("snapshot", toJson(snap).toString()).apply()
    }

    fun table(context: Context): List<TableRow> = try {
        val arr = JSONArray(prefs(context).getString("table", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TableRow(o.optString("team"), o.optInt("points"), o.optInt("played"))
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun setTable(context: Context, rows: List<TableRow>) {
        val arr = JSONArray()
        rows.forEach { arr.put(JSONObject().put("team", it.team).put("points", it.points).put("played", it.played)) }
        prefs(context).edit().putString("table", arr.toString()).apply()
    }

    // ---------------------------------------------------------------- F1 calendar (bundled, offline)

    /** A real 2026-style Grand Prix calendar; the next race is computed from the current date. */
    private val F1_CALENDAR = listOf(
        Triple("Bahrain GP", "Sakhir", "2026-03-08"),
        Triple("Saudi Arabian GP", "Jeddah", "2026-03-15"),
        Triple("Australian GP", "Melbourne", "2026-03-29"),
        Triple("Japanese GP", "Suzuka", "2026-04-05"),
        Triple("Chinese GP", "Shanghai", "2026-04-19"),
        Triple("Miami GP", "Miami", "2026-05-03"),
        Triple("Emilia-Romagna GP", "Imola", "2026-05-17"),
        Triple("Monaco GP", "Monaco", "2026-05-24"),
        Triple("Spanish GP", "Barcelona", "2026-06-07"),
        Triple("Canadian GP", "Montreal", "2026-06-14"),
        Triple("British GP", "Silverstone", "2026-07-05"),
        Triple("Belgian GP", "Spa", "2026-07-26"),
        Triple("Hungarian GP", "Budapest", "2026-08-02"),
        Triple("Dutch GP", "Zandvoort", "2026-08-30"),
        Triple("Italian GP", "Monza", "2026-09-06"),
        Triple("Singapore GP", "Marina Bay", "2026-09-20"),
        Triple("United States GP", "Austin", "2026-10-18"),
        Triple("Mexican GP", "Mexico City", "2026-10-25"),
        Triple("São Paulo GP", "Interlagos", "2026-11-08"),
        Triple("Las Vegas GP", "Las Vegas", "2026-11-21"),
        Triple("Qatar GP", "Lusail", "2026-11-29"),
        Triple("Abu Dhabi GP", "Yas Marina", "2026-12-06")
    )

    data class Race(val name: String, val circuit: String, val date: String, val daysAway: Int)

    /** The next race on or after today, wrapping to next season's opener when the calendar is done. */
    fun nextRace(context: Context, series: String = "F1"): Race {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = fmt.parse(fmt.format(Date()))!!
        val upcoming = F1_CALENDAR.firstOrNull { !fmt.parse(it.third)!!.before(today) }
            ?: F1_CALENDAR.first()
        val raceDate = fmt.parse(upcoming.third)!!
        val days = ((raceDate.time - today.time) / 86_400_000L).toInt()
        return Race(upcoming.first, upcoming.second, upcoming.third, days)
    }

    // ---------------------------------------------------------------- json helpers

    private fun toJson(s: TeamSnapshot): JSONObject = JSONObject()
        .put("team", s.team).put("opponent", s.opponent).put("league", s.league)
        .put("home", s.homeScore).put("away", s.awayScore).put("live", s.live).put("kickoff", s.kickoffMs)
        .put("form", s.form.joinToString(""))

    private fun parseSnapshot(o: JSONObject): TeamSnapshot = TeamSnapshot(
        team = o.optString("team"),
        opponent = o.optString("opponent"),
        league = o.optString("league"),
        homeScore = o.optInt("home"),
        awayScore = o.optInt("away"),
        live = o.optBoolean("live"),
        kickoffMs = o.optLong("kickoff"),
        form = o.optString("form").map { it }
    )
}
