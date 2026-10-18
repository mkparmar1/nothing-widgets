package com.example.data.media

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import com.example.core.bitmap.BitmapCache
import com.example.widgets.base.WidgetRenderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Notification access is the only way Android lets an app see other apps' media sessions.
 * This service never looks at notifications; it just watches media sessions and redraws the
 * widgets when the track or play/pause state changes.
 */
class MediaListenerService : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())
    private val watched = mutableListOf<MediaController>()
    private val refresh = Runnable {
        BitmapCache.clear()
        CoroutineScope(Dispatchers.IO).launch { WidgetRenderEngine.updateAllPlacedWidgets(applicationContext) }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = scheduleRefresh()
        override fun onPlaybackStateChanged(state: PlaybackState?) = scheduleRefresh()
        override fun onSessionDestroyed() = scheduleRefresh()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        watch(controllers.orEmpty())
        scheduleRefresh()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val me = ComponentName(this, MediaListenerService::class.java)
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, me, handler)
            watch(msm.getActiveSessions(me))
        } catch (_: SecurityException) {
        }
        scheduleRefresh()
    }

    override fun onListenerDisconnected() {
        runCatching {
            (getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager).removeOnActiveSessionsChangedListener(sessionsListener)
        }
        watch(emptyList())
        super.onListenerDisconnected()
    }

    private fun watch(controllers: List<MediaController>) {
        watched.forEach { it.unregisterCallback(controllerCallback) }
        watched.clear()
        controllers.forEach {
            it.registerCallback(controllerCallback, handler)
            watched += it
        }
    }

    /** Players fire several events per change; coalesce them into one redraw. */
    private fun scheduleRefresh() {
        handler.removeCallbacks(refresh)
        handler.postDelayed(refresh, 400)
    }
}
