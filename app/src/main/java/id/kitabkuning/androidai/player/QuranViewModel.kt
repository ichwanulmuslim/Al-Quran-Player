package id.kitabkuning.androidai.player

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import id.kitabkuning.androidai.data.QuranRepository
import id.kitabkuning.androidai.data.Reciter
import id.kitabkuning.androidai.data.SettingsRepository
import id.kitabkuning.androidai.data.Surah
import id.kitabkuning.androidai.data.SurahDetail
import id.kitabkuning.androidai.data.SurahSearch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Snapshot of the player exposed to the UI. */
data class PlayerUiState(
    val currentSurah: Surah? = null,
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isBuffering: Boolean = false,
    val hasMedia: Boolean = false,
    val error: String? = null,
)

/** Lazily-fetched verse/translation state for the Now Playing screen. */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Content(val detail: SurahDetail) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    val surahs: StateFlow<List<Surah>> = _surahs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredSurahs: StateFlow<List<Surah>> =
        combine(_surahs, _searchQuery) { list, query -> SurahSearch.filter(list, query) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _reciter = MutableStateFlow(Reciter.DEFAULT)
    val reciter: StateFlow<Reciter> = _reciter.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private val _detailState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val detailState: StateFlow<DetailUiState> = _detailState.asStateFlow()

    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var detailJob: Job? = null
    private var lastDetailNumber: Int? = null
    private var pendingPlayNumber: Int? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayerState()
            // Start/stop the position polling loop to match playback.
            startPositionUpdates()
        }
        override fun onPlaybackStateChanged(playbackState: Int) = updatePlayerState()
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) = updatePlayerState()

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            clearError()
            updatePlayerState()
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerState.update { it.copy(error = mapPlaybackError(error), isPlaying = false) }
        }
    }

    init {
        viewModelScope.launch {
            // Restore the qari before connecting so the queue is built for the
            // saved reciter instead of always starting with the default.
            _reciter.value = loadSavedReciter()
            val index = try {
                repository.loadIndex()
            } catch (e: Exception) {
                emptyList()
            }
            _surahs.value = index
            connectController(index)
        }
    }

    private suspend fun loadSavedReciter(): Reciter = try {
        settingsRepository.settings.first().reciter
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Reciter.DEFAULT
    }

    private suspend fun connectController(index: List<Surah>) {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val connected = try {
            MediaController.Builder(context, token).buildAsync().await()
        } catch (e: Exception) {
            _playerState.update { it.copy(error = "Gagal menghubungkan pemutar audio.") }
            return
        }
        controller = connected
        connected.addListener(playerListener)
        val selectedReciter = _reciter.value
        if (index.isNotEmpty()) {
            if (connected.mediaItemCount == 0) {
                connected.setMediaItems(buildSurahQueue(index, selectedReciter))
            } else if (selectedReciter != Reciter.DEFAULT) {
                // The service pre-built the default (Mishary) queue. If the user
                // picked another qari before the controller connected, rebuild
                // for that reciter, preserving the current item and position.
                val currentIndex = connected.currentMediaItemIndex.coerceAtLeast(0)
                val currentPosition = connected.currentPosition.coerceAtLeast(0L)
                connected.setMediaItems(
                    buildSurahQueue(index, selectedReciter),
                    currentIndex,
                    currentPosition,
                )
                connected.prepare()
            }
        }
        updatePlayerState()
        startPositionUpdates()

        pendingPlayNumber?.let { number ->
            pendingPlayNumber = null
            startPlayback(number)
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
        // One immediate snapshot so the seekbar is correct even while paused;
        // player-state listener callbacks also emit immediate updates.
        updatePlayerState()
        // Poll only while actually playing to avoid idle/background battery drain.
        if (controller?.isPlaying != true) return
        positionJob = viewModelScope.launch {
            while (isActive) {
                delay(500)
                updatePlayerState()
            }
        }
    }

    private fun updatePlayerState() {
        val c = controller ?: return
        val index = c.currentMediaItemIndex
        val surah = if (index >= 0) _surahs.value.getOrNull(index) else null
        val duration = if (c.duration == C.TIME_UNSET) 0L else c.duration.coerceAtLeast(0L)
        _playerState.update {
            it.copy(
                currentSurah = surah ?: it.currentSurah,
                isPlaying = c.isPlaying,
                playbackState = c.playbackState,
                positionMs = c.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                bufferedPositionMs = c.bufferedPosition.coerceAtLeast(0L),
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                hasMedia = c.mediaItemCount > 0,
            )
        }
    }

    private fun startPlayback(number: Int) {
        val c = controller ?: return
        clearError()
        c.seekTo(number - 1, 0L)
        c.play()
    }

    // ---- Public UI actions -------------------------------------------------

    fun playSurah(number: Int) {
        // Optimistic header so Now Playing renders before the controller is ready.
        _surahs.value.getOrNull(number - 1)?.let { surah ->
            _playerState.update { it.copy(currentSurah = surah) }
        }
        if (controller == null) {
            pendingPlayNumber = number
            return
        }
        startPlayback(number)
    }

    fun togglePlayPause() {
        val c = controller ?: return
        clearError()
        if (c.isPlaying) {
            c.pause()
        } else {
            when (c.playbackState) {
                // prepare() is a no-op in STATE_ENDED, so restart the current
                // surah from the beginning instead; play() alone would do nothing.
                Player.STATE_ENDED -> c.seekTo(c.currentMediaItemIndex, 0L)
                Player.STATE_IDLE -> c.prepare()
            }
            c.play()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun next() {
        clearError()
        controller?.seekToNext()
    }

    fun previous() {
        clearError()
        controller?.seekToPrevious()
    }

    fun setReciter(reciter: Reciter) {
        if (reciter == _reciter.value) return
        persistReciter(reciter)
        val c = controller ?: run { _reciter.value = reciter; return }
        val index = _surahs.value
        if (index.isEmpty()) {
            _reciter.value = reciter
            return
        }
        val currentIndex = c.currentMediaItemIndex.coerceAtLeast(0)
        val currentPosition = c.currentPosition.coerceAtLeast(0L)
        val wasPlaying = c.isPlaying
        clearError()
        c.setMediaItems(buildSurahQueue(index, reciter), currentIndex, currentPosition)
        c.prepare()
        if (wasPlaying) c.play()
        _reciter.value = reciter
        updatePlayerState()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun persistReciter(reciter: Reciter) {
        viewModelScope.launch { settingsRepository.setReciter(reciter) }
    }

    fun loadDetail(number: Int) {
        if (number == lastDetailNumber && _detailState.value is DetailUiState.Content) return
        lastDetailNumber = number
        detailJob?.cancel()
        _detailState.value = DetailUiState.Loading
        detailJob = viewModelScope.launch {
            _detailState.value = try {
                DetailUiState.Content(repository.loadDetail(number))
            } catch (e: Exception) {
                DetailUiState.Error("Gagal memuat ayat. Periksa koneksi internet Anda.")
            }
        }
    }

    fun retryDetail() {
        lastDetailNumber = null
        currentSurahNumber()?.let { loadDetail(it) }
    }

    fun dismissError() = clearError()

    private fun currentSurahNumber(): Int? =
        controller?.currentMediaItem?.mediaId?.toIntOrNull()
            ?: _playerState.value.currentSurah?.number

    private fun clearError() {
        _playerState.update { it.copy(error = null) }
    }

    private fun mapPlaybackError(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        -> "Gagal memutar audio. Periksa koneksi internet Anda."
        else -> "Tidak dapat memutar surah ini."
    }

    override fun onCleared() {
        positionJob?.cancel()
        detailJob?.cancel()
        controller?.run {
            removeListener(playerListener)
            release()
        }
        controller = null
        super.onCleared()
    }
}