/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.uamp.model;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.test.mock.MockResources;

import com.example.android.uamp.TestSetupHelper;
import com.example.android.uamp.utils.MediaIDHelper;
import com.example.android.uamp.utils.SimpleMusicProviderSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Android instrumentation unit tests for {@link MusicProvider} and related classes.
 */
@RunWith(AndroidJUnit4.class)
public class MusicProviderTest {

    private MusicProvider provider;

    @Before
    public void setupMusicProvider() throws Exception {
        SimpleMusicProviderSource source = new SimpleMusicProviderSource();
        source.add("Music 1", "Album 1", "Smith Singer", "Genre 1",
                "https://examplemusic.com/music1.mp3", "https://icons.com/album1.png", 1, 3, 3200);
        source.add("Music 2", "Album 1", "Joe Singer", "Genre 1",
                "https://examplemusic.com/music2.mp3", "https://icons.com/album1.png", 2, 3, 3300);
        source.add("Music 3", "Album 1", "John Singer", "Genre 1",
                "https://examplemusic.com/music3.mp3", "https://icons.com/album1.png", 3, 3, 3400);
        source.add("Romantic Song 1", "Album 2", "Joe Singer", "Genre 2",
                "https://examplemusic.com/music4.mp3", "https://icons.com/album2.png", 1, 2, 4200);
        source.add("Romantic Song 2", "Album 2", "Joe Singer", "Genre 2",
                "https://examplemusic.com/music5.mp3", "https://icons.com/album2.png", 2, 2, 4200);
        provider = TestSetupHelper.setupMusicProvider(source);
    }

    @Test
    public void testGetGenres() throws Exception {
        Iterable<String> genres = provider.getGenres();
        ArrayList<String> list = new ArrayList<>();
        for (String genre: genres) {
            list.add(genre);
        }
        assertEquals(2, list.size());

        Collections.sort(list);
        assertEquals(Arrays.asList(new String[]{"Genre 1", "Genre 2"}), list);
    }

    @Test
    public void testGetMusicsByGenre() throws Exception {
        int count = 0;
        for (MediaMetadataCompat metadata: provider.getMusicsByGenre("Genre 1")) {
            String genre = metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
            assertEquals("Genre 1", genre);
            count++;
        }

        assertEquals(3, count);
    }

    @Test
    public void testGetMusicsByInvalidGenre() throws Exception {
        assertFalse(provider.getMusicsByGenre("XYZ").iterator().hasNext());
    }

    @Test
    public void testSearchBySongTitle() throws Exception {
        int count = 0;
        for (MediaMetadataCompat metadata: provider.searchMusicBySongTitle("Romantic")) {
            String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            assertTrue(title.contains("Romantic"));
            count++;
        }

        assertEquals(2, count);
    }

    @Test
    public void testSearchByInvalidSongTitle() throws Exception {
        assertFalse(provider.searchMusicBySongTitle("XYZ").iterator().hasNext());
    }

    @Test
    public void testSearchMusicByAlbum() throws Exception {
        int count = 0;
        for (MediaMetadataCompat metadata: provider.searchMusicByAlbum("Album")) {
            String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            assertTrue(title.contains("Album"));
            count++;
        }

        assertEquals(5, count);
    }

    @Test
    public void testSearchMusicByInvalidAlbum() throws Exception {
        assertFalse(provider.searchMusicByAlbum("XYZ").iterator().hasNext());
    }

    @Test
    public void testSearchMusicByArtist() throws Exception {
        int count = 0;
        for (MediaMetadataCompat metadata : provider.searchMusicByArtist("Joe")) {
            String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            assertTrue(title.contains("Joe"));
            count++;
        }

        assertEquals(3, count);
    }

    @Test
    public void testSearchMusicByInvalidArtist() throws Exception {
        assertFalse(provider.searchMusicByArtist("XYZ").iterator().hasNext());
    }

    @Test
    public void testUpdateMusicArt() throws Exception {
        Bitmap bIcon = Bitmap.createBitmap(2, 2, Bitmap.Config.ALPHA_8);
        Bitmap bArt = Bitmap.createBitmap(2, 2, Bitmap.Config.ALPHA_8);

        MediaMetadataCompat metadata = provider.getShuffledMusic().iterator().next();
        String musicId = metadata.getDescription().getMediaId();

        assertNotEquals(bArt, metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
        assertNotEquals(bIcon, metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON));

        provider.updateMusicArt(musicId, bArt, bIcon);
        MediaMetadataCompat newMetadata = provider.getMusic(musicId);
        assertEquals(bArt, newMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART));
        assertEquals(bIcon, newMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON));
    }

    @Test
    public void testFavorite() throws Exception {
        MediaMetadataCompat metadata = provider.getShuffledMusic().iterator().next();
        String musicId = metadata.getDescription().getMediaId();

        assertFalse(provider.isFavorite(musicId));
        provider.setFavorite(musicId, true);
        assertTrue(provider.isFavorite(musicId));
        provider.setFavorite(musicId, false);
        assertFalse(provider.isFavorite(musicId));
    }

    @Test
    public void testGetChildren() throws Exception {
        MockResources resources = new MockResources() {
            @NonNull
            @Override
            public String getString(int id) throws NotFoundException {
                return "";
            }
            @NonNull
            @Override
            public String getString(int id, Object... formatArgs) throws NotFoundException {
                return "";
            }
        };

        // test an invalid root
        List<MediaBrowserCompat.MediaItem> invalid = provider.getChildren(
                "INVALID_MEDIA_ID", resources);
        assertEquals(0, invalid.size());

        // test level 1 (list of category types - only "by genre" for now)
        List<MediaBrowserCompat.MediaItem> level1 = provider.getChildren(
                MediaIDHelper.MEDIA_ID_ROOT, resources);
        assertEquals(1, level1.size());

        // test level 2 (list of genres)
        int genreCount = 0;
        for (String ignored : provider.getGenres()) {
            genreCount++;
        }
        List<MediaBrowserCompat.MediaItem> level2 = provider.getChildren(
                level1.get(0).getMediaId(), resources);
        assertEquals(genreCount, level2.size());

        // test level 3 (list of music for a given genre)
        List<MediaBrowserCompat.MediaItem> level3 = provider.getChildren(
                level2.get(0).getMediaId(), resources);
        String genre = MediaIDHelper.extractBrowseCategoryValueFromMediaID(
                level2.get(0).getMediaId());
        for (MediaBrowserCompat.MediaItem mediaItem: level3) {
            assertTrue(mediaItem.isPlayable());
            assertFalse(mediaItem.isBrowsable());
            MediaMetadataCompat metadata = provider.getMusic(
                    MediaIDHelper.extractMusicIDFromMediaID(mediaItem.getMediaId()));
            assertEquals(genre, metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE));
        }

        // test an invalid level 4
        List<MediaBrowserCompat.MediaItem> invalidLevel4 = provider.getChildren(
                level3.get(0).getMediaId(), resources);
        assertTrue(invalidLevel4.isEmpty());
   }
}