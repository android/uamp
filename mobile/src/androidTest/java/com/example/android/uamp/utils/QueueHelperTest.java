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
package com.example.android.uamp.utils;

import android.os.Bundle;
import android.provider.MediaStore;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.example.android.uamp.TestSetupHelper;
import com.example.android.uamp.model.MusicProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class QueueHelperTest {

    private MusicProvider provider;

    @Before
    public void setupMusicProvider() throws Exception {
        SimpleMusicProviderSource source = new SimpleMusicProviderSource();
        populateMusicSource(source);
        provider = TestSetupHelper.setupMusicProvider(source);
    }

    private void populateMusicSource(SimpleMusicProviderSource source) {
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
    }

    @Test
    public void testGetPlayingQueueForSelectedPlayableMedia() throws Exception {
        MediaMetadataCompat selectedMusic = provider.getMusicsByGenre("Genre 1").iterator().next();
        String selectedGenre = selectedMusic.getString(MediaMetadataCompat.METADATA_KEY_GENRE);

        assertEquals("Genre 1", selectedGenre);

        String mediaId = MediaIDHelper.createMediaID(
                selectedMusic.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE, selectedGenre);

        List<MediaSessionCompat.QueueItem> queue = QueueHelper.getPlayingQueue(mediaId, provider);
        assertNotNull(queue);
        assertFalse(queue.isEmpty());

        // sort by music title to simplify assertions below
        Collections.sort(queue, new Comparator<MediaSessionCompat.QueueItem>() {
            @Override
            public int compare(MediaSessionCompat.QueueItem lhs, MediaSessionCompat.QueueItem rhs) {
                return String.valueOf(lhs.getDescription().getTitle()).compareTo(
                        String.valueOf(rhs.getDescription().getTitle()));
            }
        });

        // assert they are all of the expected genre
        for (MediaSessionCompat.QueueItem item : queue) {
            String musicId = MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId());
            MediaMetadataCompat metadata = provider.getMusic(musicId);
            assertEquals(selectedGenre, metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE));
        }

        // assert that all the tracks are what we expect
        assertEquals("Music 1", queue.get(0).getDescription().getTitle());
        assertEquals("Music 2", queue.get(1).getDescription().getTitle());
        assertEquals("Music 3", queue.get(2).getDescription().getTitle());
    }

    @Test
    public void testGetPlayingQueueFromUnstructuredSearch() throws Exception {
        List<MediaSessionCompat.QueueItem> queue = QueueHelper.getPlayingQueueFromSearch(
                "Romantic", null, provider);
        assertNotNull(queue);
        assertFalse(queue.isEmpty());

        // assert they all contain "Romantic" in the title
        for (MediaSessionCompat.QueueItem item : queue) {
            String musicId = MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId());
            MediaMetadataCompat metadata = provider.getMusic(musicId);
            assertTrue(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE).contains("Romantic"));
        }
    }

    @Test
    public void testGetPlayingQueueFromArtistSearch() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE);
        extras.putString(MediaStore.EXTRA_MEDIA_ARTIST, "Joe");
        List<MediaSessionCompat.QueueItem> queue = QueueHelper.getPlayingQueueFromSearch(
                "Joe", extras, provider);
        assertNotNull(queue);
        assertFalse(queue.isEmpty());

        // assert they all contain "Joe" in the artist
        for (MediaSessionCompat.QueueItem item : queue) {
            String musicId = MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId());
            MediaMetadataCompat metadata = provider.getMusic(musicId);
            assertTrue(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST).contains("Joe"));
        }
    }

    @Test
    public void testGetMusicIndexOnQueue() throws Exception {
        // get a queue with all songs with "c" in their title
        List<MediaSessionCompat.QueueItem> queue = QueueHelper.getPlayingQueueFromSearch("c", null, provider);

        assertNotNull(queue);
        assertFalse(queue.isEmpty());

        for (int i=0; i< queue.size(); i++) {
            MediaSessionCompat.QueueItem item = queue.get(i);
            assertEquals(i, QueueHelper.getMusicIndexOnQueue(queue, item.getDescription().getMediaId()));
            assertEquals(i, QueueHelper.getMusicIndexOnQueue(queue, item.getQueueId()));
        }
    }

    @Test
    public void testGetRandomQueue() throws Exception {
        List<MediaSessionCompat.QueueItem> queue = QueueHelper.getRandomQueue(provider);
        assertNotNull(queue);
        assertFalse(queue.isEmpty());
    }

    @Test
    public void testIsIndexPlayable() throws Exception {
        // get a queue with all songs with "c" on its title
        List<MediaSessionCompat.QueueItem> queue = QueueHelper.getPlayingQueueFromSearch("c", null, provider);

        assertFalse(QueueHelper.isIndexPlayable(-1, queue));
        assertFalse(QueueHelper.isIndexPlayable(queue.size(), queue));
        assertFalse(QueueHelper.isIndexPlayable(Integer.MAX_VALUE, queue));

        if (!queue.isEmpty()) {
            assertTrue(QueueHelper.isIndexPlayable(queue.size() - 1, queue));
            assertTrue(QueueHelper.isIndexPlayable(0, queue));
        }
    }
}