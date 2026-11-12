package id.kitabkuning.androidai.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import id.kitabkuning.androidai.data.Reciter
import id.kitabkuning.androidai.data.Surah

/**
 * Builds the ordered queue of all 114 surahs as [MediaItem]s for a given qari.
 * mediaId = surah number; uri = full-surah mp3 for that reciter. Shared by the
 * service (initial queue) and the ViewModel (reciter switches).
 */
fun buildSurahQueue(surahs: List<Surah>, reciter: Reciter): List<MediaItem> =
    surahs.map { surah ->
        MediaItem.Builder()
            .setMediaId(surah.number.toString())
            .setUri(reciter.url(surah.number))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(surah.name)
                    .setArtist(reciter.displayName)
                    .setAlbumTitle("Quran Player")
                    .setDisplayTitle(surah.name)
                    .setSubtitle(surah.arabicName)
                    .build()
            )
            .build()
    }