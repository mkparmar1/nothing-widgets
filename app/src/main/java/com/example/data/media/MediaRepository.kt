package com.example.data.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock

/** Snapshot of the media session that's currently playing (or was last active). */
data class NowPlaying(
    val title: String,
    val artist: String,
    val appPackage: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    /** Small copy of the album art, if the player provides one. */
    val art: Bitmap?
)

/**
 * Reads and controls media sessions. Works whenever the user has granted notification access to
 * [MediaListenerService]; otherwise everything returns null / does nothing.
 */
object MediaRepository {

    private fun controllers(context: Context): List<MediaController> = try {
        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        msm.getActiveSessions(ComponentName(context, MediaListenerService::class.java))
    } catch (_: SecurityException) {
        emptyList()
    }

    /** Prefer a session that's playing; otherwise the most recent one. */
    private fun activeController(context: Context): MediaController? {
        val all = controllers(context)
        return all.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: all.firstOrNull()
    }

    fun nowPlaying(context: Context): NowPlaying? {
        val controller = activeController(context) ?: return null
        val meta = controller.metadata ?: return null
        val state = controller.playbackState
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: return null
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""

        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        // Extrapolate the position since the player last reported it.
        val position = state?.let {
            if (isPlaying) it.position + ((SystemClock.elapsedRealtime() - it.lastPositionUpdateTime) * it.playbackSpeed).toLong()
            else it.position
        } ?: 0L
        val duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION)

        val art = (meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON))
            ?.let { runCatching { Bitmap.createScaledBitmap(it, 48, 48, true) }.getOrNull() }

        return NowPlaying(title, artist, controller.packageName, isPlaying, position.coerceIn(0, duration.coerceAtLeast(0)), duration, art)
    }

    enum class Command { PLAY_PAUSE, NEXT, PREVIOUS }

    fun send(context: Context, command: Command) {
        val controller = activeController(context) ?: return
        val controls = controller.transportControls
        when (command) {
            Command.PLAY_PAUSE ->
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) controls.pause() else controls.play()
            Command.NEXT -> controls.skipToNext()
            Command.PREVIOUS -> controls.skipToPrevious()
        }
    }
}
