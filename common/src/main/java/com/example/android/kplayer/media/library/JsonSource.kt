/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.kplayer.media.library
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
import android.support.v4.media.MediaMetadataCompat
import com.example.android.kplayer.media.R
import com.example.android.kplayer.media.extensions.album
import com.example.android.kplayer.media.extensions.albumArtUri
import com.example.android.kplayer.media.extensions.artist
import com.example.android.kplayer.media.extensions.displayDescription
import com.example.android.kplayer.media.extensions.displayIconUri
import com.example.android.kplayer.media.extensions.displaySubtitle
import com.example.android.kplayer.media.extensions.displayTitle
import com.example.android.kplayer.media.extensions.downloadStatus
import com.example.android.kplayer.media.extensions.duration
import com.example.android.kplayer.media.extensions.flag
import com.example.android.kplayer.media.extensions.genre
import com.example.android.kplayer.media.extensions.id
import com.example.android.kplayer.media.extensions.mediaUri
import com.example.android.kplayer.media.extensions.title
import com.example.android.kplayer.media.extensions.trackCount
import com.example.android.kplayer.media.extensions.trackNumber
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Source of [MediaMetadataCompat] objects created from a basic JSON stream.
 *
 * The definition of the JSON is specified in the docs of [JsonMusic] in this file,
 * which is the object representation of it.
 */
class JsonSource(private val source: Uri, private val context: Context) : AbstractMusicSource() {

    private var catalog: List<MediaMetadataCompat> = emptyList()
    private var localCatalog: List<MediaMetadataCompat> = emptyList()

    init {
        state = STATE_INITIALIZING
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()

    override suspend fun load() {
        updateCatalog(source)?.let { updatedCatalog ->
            catalog = updatedCatalog
            state = STATE_INITIALIZED
        } ?: run {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

//    override suspend fun loadLocal() {
//        updateLocalCatalog(context)?.let { updateLocalCatalog ->
//            localCatalog = updateLocalCatalog
////            localState = 3
//        } ?: run {
//            localCatalog = emptyList()
//            localState = STATE_ERROR
//        }
//    }

    /**
     * Function to connect to a remote URI and download/process the JSON file that corresponds to
     * [MediaMetadataCompat] objects.
     */
    private suspend fun updateCatalog(catalogUri: Uri): List<MediaMetadataCompat>? {
        return withContext(Dispatchers.IO) {
            val musicCat = try {
                downloadJson(catalogUri)
            } catch (ioException: IOException) {
                return@withContext null
            }



            // Get the base URI to fix up relative references later.
            val baseUri = catalogUri.toString().removeSuffix(catalogUri.lastPathSegment ?: "")

            musicCat.music += fetchSongs(context)

            val mediaMetadataCompats = musicCat.music.map { song ->
                // The JSON may have paths that are relative to the source of the JSON
                // itself. We need to fix them up here to turn them into absolute paths.
                catalogUri.scheme?.let { scheme ->
                    if (!song.album.equals("MY SONGS")) {
                        if (!song.source.startsWith(scheme)) {
                            song.source = baseUri + song.source
                        }
                        if (!song.image.startsWith(scheme)) {
                            song.image = baseUri + song.image
                        }
                    }
                }
                val imageUri = AlbumArtContentProvider.mapUri(Uri.parse(song.image))

                MediaMetadataCompat.Builder()
                    .from(song)
                    .apply {
                        if(!song.album.equals("MY SONGS")) {
                            displayIconUri =
                                imageUri.toString() // Used by ExoPlayer and Notification
                            albumArtUri = imageUri.toString()
                        }
                    }
                    .build()
            }.toList()
            // Add description keys to be used by the ExoPlayer MediaSession extension when
            // announcing metadata changes.
            mediaMetadataCompats.forEach { it.description.extras?.putAll(it.bundle) }
            mediaMetadataCompats
        }
    }

    @Throws(FileNotFoundException::class)
    private fun fetchSongs(context: Context) : List<JsonMusic>{
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 "
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )
//    songs = ArrayList()
        val songs: MutableList<JsonMusic> = ArrayList()
        val titleColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val songArtist: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val songId: Int = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        while (cursor.moveToNext()) {
            val art: Bitmap? = getRawByt(context, cursor.getLong(songId))
            val song = JsonMusic()
            if (null != art) {
//            song.setSongImage(art)
            }
            song.artist = cursor.getString(songArtist)
            song.id = cursor.getLong(songId).toString()
            song.title = cursor.getString(titleColumn)
            song.album = "MY SONGS"
            song.source = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(songId)).toString();
            song.image = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(songId)).toString()
            songs.add(song)
        }

        return  songs
    }

    private suspend fun updateLocalCatalog( context: Context): List<MediaMetadataCompat>? {
        return withContext(Dispatchers.IO) {
            val musicCat = try {
                fetchSongs(context)
            } catch (ioException: IOException) {
                return@withContext null
            }

            val mediaMetadataCompats = musicCat?.map { song ->
                // The JSON may have paths that are relative to the source of the JSON
                // itself. We need to fix them up here to turn them into absolute paths.
        //                catalogUri.scheme?.let { scheme ->
        //                    if (!song.source.startsWith(scheme)) {
        //                        song.source = baseUri + song.source
        //                    }
        //                    if (!song.image.startsWith(scheme)) {
        //                        song.image = baseUri + song.image
        //                    }
        //                }
        //                val imageUri = AlbumArtContentProvider.mapUri(Uri.parse(song.image))

                MediaMetadataCompat.Builder()
                    .from(song)
                    .apply {
                        displayIconUri = song.image.toString() // Used by ExoPlayer and Notification
                        albumArtUri = song.image.toString()
                    }
                    .build()
            }?.toList()
            // Add description keys to be used by the ExoPlayer MediaSession extension when
            // announcing metadata changes.
            mediaMetadataCompats?.forEach { it.description.extras?.putAll(it.bundle) }
            mediaMetadataCompats
        }
    }

    /**
     * Attempts to download a catalog from a given Uri.
     *
     * @param catalogUri URI to attempt to download the catalog form.
     * @return The catalog downloaded, or an empty catalog if an error occurred.
     */
    @Throws(IOException::class)
    private fun downloadJson(catalogUri: Uri): JsonCatalog {
        val catalogConn = URL(catalogUri.toString())
        val reader = BufferedReader(InputStreamReader(catalogConn.openStream()))
        return Gson().fromJson(reader, JsonCatalog::class.java)
    }
}

@Throws(FileNotFoundException::class)
private fun fetchSongs(context: Context) : List<JsonMusic>?{
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DURATION
    )
    val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 "
    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        null
    )
//    songs = ArrayList()
    val songs: MutableList<JsonMusic> = ArrayList()
    val titleColumn: Int = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
    val songArtist: Int = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
    val songId: Int = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
    while (cursor.moveToNext()) {
        val art: Bitmap? = getRawByt(context, cursor.getLong(songId))
        val song = JsonMusic()
        if (null != art) {
//            song.setSongImage(art)
        }
        song.artist = cursor.getString(songArtist)
        song.id = cursor.getLong(songId).toString()
        song.title = cursor.getString(titleColumn)
        song.image = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(songId)).toString()
        songs.add(song)
    }

    return  songs
}

private fun getRawByt(context: Context, albumId: Long): Bitmap? {
    val mmr = MediaMetadataRetriever()
    val rawArt: ByteArray?
    var art: Bitmap? = null
    val bfo = BitmapFactory.Options()
    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, albumId)
    try {
        mmr.setDataSource(context, uri)
        rawArt = mmr.embeddedPicture
        if (null != rawArt) {
            art = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, bfo)
        }
    } catch (e: java.lang.Exception) {
        art = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_art )
        //            Log.i("error", e.getMessage());
    }
    return art
}

/**
 * Extension method for [MediaMetadataCompat.Builder] to set the fields from
 * our JSON constructed object (to make the code a bit easier to see).
 */
fun  MediaMetadataCompat.Builder.from(jsonMusic: JsonMusic): MediaMetadataCompat.Builder {
    // The duration from the JSON is given in seconds, but the rest of the code works in
    // milliseconds. Here's where we convert to the proper units.
    val durationMs = TimeUnit.SECONDS.toMillis(jsonMusic.duration)

    id = jsonMusic.id
    title = jsonMusic.title
    artist = jsonMusic.artist
    album = jsonMusic.album
    duration = durationMs
    genre = jsonMusic.genre
    mediaUri = jsonMusic.source
    albumArtUri = jsonMusic.image
    trackNumber = jsonMusic.trackNumber
    trackCount = jsonMusic.totalTrackCount
    flag = MediaItem.FLAG_PLAYABLE

    // To make things easier for *displaying* these, set the display properties as well.
    displayTitle = jsonMusic.title
    displaySubtitle = jsonMusic.artist
    displayDescription = jsonMusic.album
    displayIconUri = jsonMusic.image

    // Add downloadStatus to force the creation of an "extras" bundle in the resulting
    // MediaMetadataCompat object. This is needed to send accurate metadata to the
    // media session during updates.
    downloadStatus = STATUS_NOT_DOWNLOADED

    // Allow it to be used in the typical builder style.
    return this
}

//fun MediaMetadataCompat.Builder.from(jsonMusic: LocalJsonMusic): MediaMetadataCompat.Builder {
//    // The duration from the JSON is given in seconds, but the rest of the code works in
//    // milliseconds. Here's where we convert to the proper units.
//    val durationMs = TimeUnit.SECONDS.toMillis(jsonMusic.duration)
//
//    id = jsonMusic.id
//    title = jsonMusic.title
//    artist = jsonMusic.artist
//    album = jsonMusic.album
//    duration = durationMs
//    genre = jsonMusic.genre
//    mediaUri = jsonMusic.source.toString()
//    albumArtUri = jsonMusic.image.toString()
//    trackNumber = jsonMusic.trackNumber
//    trackCount = jsonMusic.totalTrackCount
//    flag = MediaItem.FLAG_PLAYABLE
//
//    // To make things easier for *displaying* these, set the display properties as well.
//    displayTitle = jsonMusic.title
//    displaySubtitle = jsonMusic.artist
//    displayDescription = jsonMusic.album
//    displayIconUri = jsonMusic.image
//
//    // Add downloadStatus to force the creation of an "extras" bundle in the resulting
//    // MediaMetadataCompat object. This is needed to send accurate metadata to the
//    // media session during updates.
//    downloadStatus = STATUS_NOT_DOWNLOADED
//
//    // Allow it to be used in the typical builder style.
//    return this
//}

/**
 * Wrapper object for our JSON in order to be processed easily by GSON.
 */
class JsonCatalog {
    var music: List<JsonMusic> = ArrayList()
}

/**
 * An individual piece of music included in our JSON catalog.
 * The format from the server is as specified:
 * ```
 *     { "music" : [
 *     { "title" : // Title of the piece of music
 *     "album" : // Album title of the piece of music
 *     "artist" : // Artist of the piece of music
 *     "genre" : // Primary genre of the music
 *     "source" : // Path to the music, which may be relative
 *     "image" : // Path to the art for the music, which may be relative
 *     "trackNumber" : // Track number
 *     "totalTrackCount" : // Track count
 *     "duration" : // Duration of the music in seconds
 *     "site" : // Source of the music, if applicable
 *     }
 *     ]}
 * ```
 *
 * `source` and `image` can be provided in either relative or
 * absolute paths. For example:
 * ``
 *     "source" : "https://www.example.com/music/ode_to_joy.mp3",
 *     "image" : "ode_to_joy.jpg"
 * ``
 *
 * The `source` specifies the full URI to download the piece of music from, but
 * `image` will be fetched relative to the path of the JSON file itself. This means
 * that if the JSON was at "https://www.example.com/json/music.json" then the image would be found
 * at "https://www.example.com/json/ode_to_joy.jpg".
 */
@Suppress("unused")
class JsonMusic {
    var id: String = ""
    var title: String = ""
    var album: String = ""
    var artist: String = ""
    var genre: String = ""
    var source: String = ""
    var image: String = ""
    var trackNumber: Long = 0
    var totalTrackCount: Long = 0
    var duration: Long = -1
    var site: String = ""
}


//@Suppress("unused")
//class LocalJsonMusic {
//    var id: String = ""
//    var title: String = ""
//    var album: String = ""
//    var artist: String = ""
//    var genre: String = ""
//    var source: Uri? = null
//    var image: Uri? = null
//    var trackNumber: Long = 0
//    var totalTrackCount: Long = 0
//    var duration: Long = -1
//    var site: String = ""
//}
