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
package com.example.android.uamp.playback;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.test.mock.MockResources;

import com.example.android.uamp.TestSetupHelper;
import com.example.android.uamp.model.MusicProvider;
import com.example.android.uamp.utils.MediaIDHelper;
import com.example.android.uamp.utils.QueueHelper;
import com.example.android.uamp.utils.SimpleMusicProviderSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Android instrumentation unit tests for {@link QueueManager} and related classes.
 */
@RunWith(AndroidJUnit4.class)
public class QueueManagerTest {

    private MusicProvider provider;

    @Before
    public void setupMusicProvider() throws Exception {
        SimpleMusicProviderSource source = new SimpleMusicProviderSource();
        populateMusicSource(source);
        provider = TestSetupHelper.setupMusicProvider(source);
    }

    private void populateMusicSource(SimpleMusicProviderSource source) {
        source.add("Music 1", "Album 1", "Smith Singer", "Genre 1",
                "https://examplemusic.com/music1.mp3", null, 1, 3, 3200);
        source.add("Music 2", "Album 1", "Joe Singer", "Genre 1",
                "https://examplemusic.com/music2.mp3", null, 2, 3, 3300);
        source.add("Music 3", "Album 1", "John Singer", "Genre 1",
                "https://examplemusic.com/music3.mp3", null, 3, 3, 3400);
        source.add("Romantic Song 1", "Album 2", "Joe Singer", "Genre 2",
                "https://examplemusic.com/music4.mp3", null, 1, 2, 4200);
        source.add("Romantic Song 2", "Album 2", "Joe Singer", "Genre 2",
                "https://examplemusic.com/music5.mp3", null, 2, 2, 4200);
    }

    private QueueManager createQueueManagerWithValidation(final CountDownLatch latch,
            final int expectedQueueIndex, final List<MediaSessionCompat.QueueItem> expectedNewQueue) {
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
        return new QueueManager(provider, resources,
                new QueueManager.MetadataUpdateListener() {
                    @Override
                    public void onMetadataChanged(MediaMetadataCompat metadata) {
                    }

                    @Override
                    public void onMetadataRetrieveError() {
                    }

                    @Override
                    public void onCurrentQueueIndexUpdated(int queueIndex) {
                        if (expectedQueueIndex >= 0) {
                            assertEquals(expectedQueueIndex, queueIndex);
                        }
                        if (latch != null) latch.countDown();
                    }

                    @Override
                    public void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue) {
                        if (expectedNewQueue != null) {
                            assertEquals(expectedNewQueue, newQueue);
                        }
                        if (latch != null) latch.countDown();
                    }
                });
    }

    @Test
    public void testIsSameBrowsingCategory() throws Exception {
        QueueManager queueManager = createQueueManagerWithValidation(null, -1, null);

        Iterator<String> genres = provider.getGenres().iterator();
        String genre1 = genres.next();
        String genre2 = genres.next();
        List<MediaSessionCompat.QueueItem> queueGenre1 = QueueHelper.getPlayingQueue(
                MediaIDHelper.createMediaID(null, MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE, genre1),
                provider);
        List<MediaSessionCompat.QueueItem> queueGenre2 = QueueHelper.getPlayingQueue(
                MediaIDHelper.createMediaID(null, MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE, genre2),
                provider);

        // set the current queue
        queueManager.setCurrentQueue("Queue genre 1", queueGenre1);

        // the current music cannot be of same browsing category as one with genre 2
        assertFalse(queueManager.isSameBrowsingCategory(queueGenre2.get(0).getDescription().getMediaId()));

        // the current music needs to be of same browsing category as one with genre 1
        assertTrue(queueManager.isSameBrowsingCategory(queueGenre1.get(0).getDescription().getMediaId()));
   }

    @Test
    public void testSetValidQueueItem() throws Exception {
        // Get a queue that contains songs with space on their title (all in our test dataset)
        List<MediaSessionCompat.QueueItem> queue = QueueHelper.getPlayingQueueFromSearch(
                " ", null, provider);

        int expectedItemIndex = queue.size() - 1;
        MediaSessionCompat.QueueItem expectedItem = queue.get(expectedItemIndex);
        // Latch for 3 tests
        CountDownLatch latch = new CountDownLatch(3);
        QueueManager queueManager = createQueueManagerWithValidation(latch, expectedItemIndex,
                queue);

        // test 1: set the current queue
        queueManager.setCurrentQueue("Queue 1", queue);

        // test 2: set queue index to the expectedItem using its queueId
        assertTrue(queueManager.setCurrentQueueItem(expectedItem.getQueueId()));

        // test 3: set queue index to the expectedItem using its mediaId
        assertTrue(queueManager.setCurrentQueueItem(expectedItem.getDescription().getMediaId()));

        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testSetInvalidQueueItem() throws Exception {
        // Get a queue that contains songs with space on their title (all in our test dataset)
        List<MediaSessionCompat.QueueItem> queue = QueueHelper.getPlayingQueueFromSearch(
                " ", null, provider);

        int expectedItemIndex = queue.size() - 1;

        // Latch for 1 test, because queueItem setters will fail and not trigger the validation
        // listener
        CountDownLatch latch = new CountDownLatch(1);
        QueueManager queueManager = createQueueManagerWithValidation(latch, expectedItemIndex,
                queue);

        // test 1: set the current queue
        queueManager.setCurrentQueue("Queue 1", queue);

        // test 2: set queue index to an invalid queueId (we assume MAX_VALUE is invalid, because
        // queueIds are, in uAmp, defined as the item's index, and no queue is big enough to have
        // a MAX_VALUE index)
        assertFalse(queueManager.setCurrentQueueItem(Integer.MAX_VALUE));

        // test 3: set queue index to an invalid negative queueId
        assertFalse(queueManager.setCurrentQueueItem(-1));

        // test 3: set queue index to the expectedItem using its mediaId
        assertFalse(queueManager.setCurrentQueueItem("INVALID_MEDIA_ID"));

        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testSkip() throws Exception {
        // Get a queue that contains songs with space on their title (all in our test dataset)
        List<MediaSessionCompat.QueueItem> queue = QueueHelper.getPlayingQueueFromSearch(
                " ", null, provider);
        assertTrue(queue.size() > 3);

        QueueManager queueManager = createQueueManagerWithValidation(null, -1, queue);
        queueManager.setCurrentQueue("Queue 1", queue);

        // start on index 3
        long expectedQueueId = queue.get(3).getQueueId();
        assertTrue(queueManager.setCurrentQueueItem(expectedQueueId));
        assertEquals(expectedQueueId, queueManager.getCurrentMusic().getQueueId());

        // skip to previous (expected: index 2)
        expectedQueueId = queue.get(2).getQueueId();
        assertTrue(queueManager.skipQueuePosition(-1));
        assertEquals(expectedQueueId, queueManager.getCurrentMusic().getQueueId());

        // skip twice to previous (expected: index 0)
        expectedQueueId = queue.get(0).getQueueId();
        assertTrue(queueManager.skipQueuePosition(-2));
        assertEquals(expectedQueueId, queueManager.getCurrentMusic().getQueueId());

        // skip to previous (expected: index 0, by definition)
        expectedQueueId = queue.get(0).getQueueId();
        assertTrue(queueManager.skipQueuePosition(-1));
        assertEquals(expectedQueueId, queueManager.getCurrentMusic().getQueueId());

        // skip to 2 past the last index (expected: index 1, because
        // newindex = (index + skip) % size, by definition)
        expectedQueueId = queue.get(1).getQueueId();
        assertTrue(queueManager.skipQueuePosition(queue.size() + 1));
        assertEquals(expectedQueueId, queueManager.getCurrentMusic().getQueueId());
    }

    @Test
    public void testSetQueueFromSearch() throws Exception {
        QueueManager queueManager = createQueueManagerWithValidation(null, -1, null);
        // set a queue from a free search
        queueManager.setQueueFromSearch("Romantic", null);
        // confirm that the search results have the expected size of 2 (because we know the dataset)
        assertEquals(2, queueManager.getCurrentQueueSize());

        // for each result, check if it contains the search term in its title
        for (int i=0; i < queueManager.getCurrentQueueSize(); i++) {
            MediaSessionCompat.QueueItem item = queueManager.getCurrentMusic();
            assertTrue(item.getDescription().getTitle().toString().contains("Romantic"));
            queueManager.skipQueuePosition(1);
        }
    }

    @Test
    public void testSetQueueFromMusic() throws Exception {
        QueueManager queueManager = createQueueManagerWithValidation(null, -1, null);
        // get the first music of the first genre and build a hierarchy-aware version of its
        // mediaId
        String genre = provider.getGenres().iterator().next();
        MediaMetadataCompat metadata = provider.getMusicsByGenre(genre).iterator().next();
        String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                metadata.getDescription().getMediaId(), MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE,
                genre);

        // set a queue from the hierarchyAwareMediaID. It should contain all music with the same
        // genre
        queueManager.setQueueFromMusic(hierarchyAwareMediaID);

        // count all songs with the same genre
        int count = 0;
        for (MediaMetadataCompat m: provider.getMusicsByGenre(genre)) {
            count++;
        }

        // check if size matches
        assertEquals(count, queueManager.getCurrentQueueSize());

        // Now check if all songs in current queue have the expected genre:
        for (int i=0; i < queueManager.getCurrentQueueSize(); i++) {
            MediaSessionCompat.QueueItem item = queueManager.getCurrentMusic();
            String musicId = MediaIDHelper.extractMusicIDFromMediaID(
                    item.getDescription().getMediaId());
            String itemGenre = provider.getMusic(musicId).getString(
                    MediaMetadataCompat.METADATA_KEY_GENRE);
            assertEquals(genre, itemGenre);
            queueManager.skipQueuePosition(1);
        }
    }
}