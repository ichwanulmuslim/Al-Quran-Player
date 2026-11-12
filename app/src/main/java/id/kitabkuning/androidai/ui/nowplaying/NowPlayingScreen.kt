package id.kitabkuning.androidai.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.kitabkuning.androidai.data.Reciter
import id.kitabkuning.androidai.data.Surah
import id.kitabkuning.androidai.data.Verse
import id.kitabkuning.androidai.player.DetailUiState
import id.kitabkuning.androidai.player.PlayerUiState
import id.kitabkuning.androidai.player.QuranViewModel
import id.kitabkuning.androidai.ui.formatDuration
import id.kitabkuning.androidai.ui.theme.LocalReadingStyle
import id.kitabkuning.androidai.ui.theme.arabicReadingStyle
import id.kitabkuning.androidai.ui.theme.translationReadingStyle

/**
 * Now Playing: surah header, playback controls + seekbar, qari selector, and the
 * verse list (Arabic + Indonesian translation) fetched lazily per surah. Verses
 * are for reading only — full-surah audio has no per-ayah timestamps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    surahNumber: Int,
    viewModel: QuranViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val surahs by viewModel.surahs.collectAsStateWithLifecycle()
    val reciter by viewModel.reciter.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()

    val surah = playerState.currentSurah ?: surahs.getOrNull(surahNumber - 1)

    LaunchedEffect(surah?.number) {
        surah?.number?.let { viewModel.loadDetail(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sedang Diputar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (surah == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(Modifier.padding(padding).fillMaxSize()) {
            SurahHeader(surah)
            PlaybackControls(state = playerState, viewModel = viewModel)
            ReciterSelector(selected = reciter, onSelect = viewModel::setReciter)
            HorizontalDivider()
            Text(
                "Ayat & Terjemahan Indonesia",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Box(Modifier.weight(1f)) {
                when (val state = detailState) {
                    is DetailUiState.Loading -> DetailLoading()
                    is DetailUiState.Error -> DetailError(
                        message = state.message,
                        onRetry = viewModel::retryDetail,
                    )
                    is DetailUiState.Content -> LazyColumn(Modifier.fillMaxSize()) {
                        items(state.detail.verses, key = { it.number }) { verse ->
                            VerseRow(verse)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahHeader(surah: Surah) {
    val headerSize = LocalReadingStyle.current.headerArabicSizeSp
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                surah.arabicName,
                modifier = Modifier.fillMaxWidth(),
                style = arabicReadingStyle(
                    base = MaterialTheme.typography.headlineLarge,
                    sizeSp = headerSize,
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            surah.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            surah.indonesianMeaning,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${surah.numberOfAyah} ayat • ${surah.type}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaybackControls(state: PlayerUiState, viewModel: QuranViewModel) {
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    val maxDuration = state.durationMs.toFloat().coerceAtLeast(1f)
    val displayed = (dragPosition ?: state.positionMs.toFloat()).coerceIn(0f, maxDuration)

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Slider(
            value = displayed,
            onValueChange = { dragPosition = it },
            onValueChangeFinished = {
                dragPosition?.let { viewModel.seekTo(it.toLong()) }
                dragPosition = null
            },
            valueRange = 0f..maxDuration,
            enabled = state.durationMs > 0L,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatDuration(displayed.toLong()), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelSmall)
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::previous) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Surah sebelumnya",
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.width(24.dp))
            FilledIconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.size(64.dp)) {
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Jeda" else "Putar",
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Spacer(Modifier.width(24.dp))
            IconButton(onClick = viewModel::next) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Surah berikutnya",
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun ReciterSelector(selected: Reciter, onSelect: (Reciter) -> Unit) {
    val labels = mapOf(
        Reciter.MISHARY to "Al-Afasy",
        Reciter.SUDAIS to "As-Sudais",
        Reciter.GHAMDI to "Al-Ghamdi",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Qari:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Reciter.entries.forEach { reciter ->
            FilterChip(
                selected = reciter == selected,
                onClick = { onSelect(reciter) },
                label = { Text(labels[reciter] ?: reciter.displayName) },
            )
        }
    }
}

@Composable
private fun VerseRow(verse: Verse) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    verse.number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Ayat ${verse.number}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                verse.text,
                modifier = Modifier.fillMaxWidth(),
                style = arabicReadingStyle(base = MaterialTheme.typography.bodyLarge),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            verse.translationId,
            style = translationReadingStyle(base = MaterialTheme.typography.bodyMedium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun DetailLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Memuat ayat...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Coba lagi") }
        }
    }
}