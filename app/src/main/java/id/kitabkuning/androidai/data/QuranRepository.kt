package id.kitabkuning.androidai.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Loads the offline surah index from bundled assets and lazily fetches per-surah
 * detail (verses + Indonesian translation) over HTTP with a ~50 MB disk cache.
 * The index never requires network; only detail does.
 */
class QuranRepository(private val context: Context) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "quran_detail"), 50L * 1024 * 1024))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val detailCache = HashMap<Int, SurahDetail>()

    suspend fun loadIndex(): List<Surah> = withContext(Dispatchers.IO) {
        context.assets.open("quran.json").use { input ->
            QuranJson.parseIndex(input.readBytes().decodeToString())
        }
    }

    suspend fun loadDetail(number: Int): SurahDetail = withContext(Dispatchers.IO) {
        synchronized(detailCache) { detailCache[number] }?.let { return@withContext it }

        val request = Request.Builder().url("$DETAIL_BASE/$number.json").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Respons kosong")
            val detail = QuranJson.parseDetail(body)
            synchronized(detailCache) { detailCache[number] = detail }
            detail
        }
    }

    private companion object {
        const val DETAIL_BASE =
            "https://raw.githubusercontent.com/penggguna/QuranJSON/master/surah"
    }
}