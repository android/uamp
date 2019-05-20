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
import android.support.v4.media.MediaMetadataCompat
import com.example.android.uamp.media.extensions.album
import com.example.android.uamp.media.extensions.artist
import com.example.android.uamp.media.extensions.genre
import com.example.android.uamp.media.extensions.id
import com.example.android.uamp.media.extensions.title
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

    private val musicList = listOf<MediaMetadataCompat>(
            MediaMetadataCompat.Builder().apply {
                id = "ich_hasse_dich"
                title = "Ich hasse dich"
                album = "Speechless"
                artist = "Jemand"
                genre = "Folk"
            }.build(),

            MediaMetadataCompat.Builder().apply {
                id = "about_a_guy"
                title = "About a Guy"
                album = "Tales from the Render Farm"
                artist = "7 Developers and a Pastry Chef"
                genre = "Rock"
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
        Assert.assertEquals(result[0].id, "about_a_guy")
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
        Assert.assertEquals(result[0].id, "about_a_guy")
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
        Assert.assertEquals(result[0].id, "ich_hasse_dich")
    }
}

class TestMusicSource(private val music: List<MediaMetadataCompat>
) : AbstractMusicSource(), Iterable<MediaMetadataCompat> by music {
    override suspend fun load() = Unit

    fun prepare() {
        state = STATE_INITIALIZED
    }

    fun error() {
        state = STATE_ERROR
    }
}