/*
 * Copyright 2018 Google Inc. All rights reserved.
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

package com.example.android.uamp.media.library

import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A small set of Android integration tests for (primarily) [AbstractMusicSource].
 *
 * The tests all use an extension of [AbstractMusicSource] which is defined at the
 * bottom of this file: [TestMusicSource].
 */
@RunWith(RobolectricTestRunner::class)
class MusicSourceTest {

    private val musicList = listOf<MediaItem>(
        MediaItem.Builder().apply {
            setMediaId("ich_hasse_dich")
            setMediaMetadata(MediaMetadata.Builder().apply {
                setTitle("Ich hasse dich")
                setAlbumTitle("Speechless")
                setArtist("Jemand")
                setGenre("Folk")
            }.build())
        }.build(),

        MediaItem.Builder().apply {
            setMediaId("about_a_guy")
            setMediaMetadata(MediaMetadata.Builder().apply {
                setTitle("About a Guy")
                setAlbumTitle("Tales from the Render Farm")
                setArtist("7 Developers and a Pastry Chef")
                setGenre("FoRocklk")
            }.build())
        }.build()
    )

    /** Variables for testing [MusicSource.whenReady] */
    var waiting = true

    @Test
    fun whenReady_waits() {
        val testSource = TestMusicSource(musicList)
        waiting = true

        testSource.whenReady { success ->
            Assert.assertEquals(success, true)
            waiting = false
        }
        Assert.assertEquals(waiting, true)
        testSource.prepare()
        Assert.assertEquals(waiting, false)
    }

    @Test
    fun whenReady_errorContinues() {
        val testSource = TestMusicSource(musicList)
        waiting = true

        testSource.whenReady { success ->
            Assert.assertEquals(success, false)
            waiting = false
        }
        Assert.assertEquals(waiting, true)
        testSource.error()
        Assert.assertEquals(waiting, false)
    }

    @Test
    fun searchByGenre_matches() {
        val testSource = TestMusicSource(musicList)
        testSource.prepare()

        val searchQuery = "Rock"
        val searchExtras = Bundle().apply {
            putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE)
            putString(MediaStore.EXTRA_MEDIA_GENRE, searchQuery)
        }
        val result = testSource.search(searchQuery, searchExtras)
        Assert.assertEquals(result.size, 1)
        Assert.assertEquals(result[0].mediaId, "about_a_guy")
    }

    @Test
    fun searchByMedia_matches() {
        val testSource = TestMusicSource(musicList)
        testSource.prepare()

        val searchQuery = "About a Guy"
        val searchExtras = Bundle().apply {
            putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
            putString(MediaStore.EXTRA_MEDIA_TITLE, searchQuery)
            putString(MediaStore.EXTRA_MEDIA_ALBUM, "Tales from the Render Farm")
            putString(MediaStore.EXTRA_MEDIA_ARTIST, "7 Developers and a Pastry Chef")
        }
        val result = testSource.search(searchQuery, searchExtras)
        Assert.assertEquals(result.size, 1)
        Assert.assertEquals(result[0].mediaId, "about_a_guy")
    }

    @Test
    fun searchByMedia_nomatches() {
        val testSource = TestMusicSource(musicList)
        testSource.prepare()

        val searchQuery = "Kotlin in 31 Days"
        val searchExtras = Bundle().apply {
            putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
            putString(MediaStore.EXTRA_MEDIA_TITLE, searchQuery)
            putString(MediaStore.EXTRA_MEDIA_ALBUM, "Delegated by Lazy")
            putString(MediaStore.EXTRA_MEDIA_ARTIST, "Brainiest Jet")
        }
        val result = testSource.search(searchQuery, searchExtras)
        Assert.assertEquals(result.size, 0)
    }

    @Test
    fun searchByKeyword_fallback() {
        val testSource = TestMusicSource(musicList)
        testSource.prepare()

        val searchQuery = "hasse"
        val searchExtras = Bundle.EMPTY
        val result = testSource.search(searchQuery, searchExtras)
        Assert.assertEquals(result.size, 1)
        Assert.assertEquals(result[0].mediaId, "ich_hasse_dich")
    }
}

class TestMusicSource(
    private val music: List<MediaItem>
) : AbstractMusicSource(), Iterable<MediaItem> by music {
    override suspend fun load() = Unit

    fun prepare() {
        state = STATE_INITIALIZED
    }

    fun error() {
        state = STATE_ERROR
    }
}