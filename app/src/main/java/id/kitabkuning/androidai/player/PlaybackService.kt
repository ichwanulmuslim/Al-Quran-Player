package id.kitabkuning.androidai.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import id.kitabkuning.androidai.data.QuranJson
import id.kitabkuning.androidai.data.Reciter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Background playback engine. ExoPlayer lives inside a MediaSessionService so
 * playback continues when the app is backgrounded and a media notification with
 * standard controls is shown. Audio focus is handled by Media3 defaults.
 *
 * The session is created immediately so controllers can connect without waiting
 * on I/O; the default (Mishary) queue is then filled from the bundled offline
 * index on Dispatchers.IO. The ViewModel also builds the queue from its own
 * index when it connects, so playback never depends on this load finishing.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        // Keep CPU/Wi-Fi awake while streaming with the screen off.
        player.setWakeMode(C.WAKE_MODE_NETWORK)

        mediaSession = MediaSession.Builder(this, player).build()

        serviceScope.launch {
            val surahs = try {
                val text = assets.open("quran.json").use { it.readBytes().decodeToString() }
                QuranJson.parseIndex(text)
            } catch (e: Exception) {
                emptyList()
            }
            if (surahs.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    // Player calls must run on the main thread. Skip if the
                    // service was destroyed or a controller already set a queue.
                    val session = mediaSession ?: return@withContext
                    if (session.player.mediaItemCount == 0) {
                        session.player.setMediaItems(buildSurahQueue(surahs, Reciter.DEFAULT))
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}