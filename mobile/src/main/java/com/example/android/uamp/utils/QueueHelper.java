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

import android.media.MediaMetadata;
import android.media.session.MediaSession;

import com.example.android.uamp.model.MusicProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;

/**
 * Utility class to help on queue related tasks.
 */
public class QueueHelper {

    private static final String TAG = LogHelper.makeLogTag(QueueHelper.class);

    public static final List<MediaSession.QueueItem> getPlayingQueue(String mediaId,
            MusicProvider musicProvider) {

        // extract the category and unique music ID from the media ID:
        String[] category = MediaIDHelper.extractBrowseCategoryFromMediaID(mediaId);

        // This sample only supports genre category.
        if (!category[0].equals(MEDIA_ID_MUSICS_BY_GENRE) || category.length != 2) {
            LogHelper.e(TAG, "Could not build a playing queue for this mediaId: ", mediaId);
            return null;
        }

        String categoryValue = category[1];
        LogHelper.e(TAG, "Creating playing queue for musics of genre ", categoryValue);

        List<MediaSession.QueueItem> queue = convertToQueue(
                musicProvider.getMusicsByGenre(categoryValue));

        return queue;
    }

    public static final List<MediaSession.QueueItem> getPlayingQueueFromSearch(String query,
            MusicProvider musicProvider) {

        LogHelper.e(TAG, "Creating playing queue for musics from search ", query);

        return convertToQueue(musicProvider.searchMusics(query));
    }


    public static final int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue,
             String mediaId) {
        int index = 0;
        for (MediaSession.QueueItem item : queue) {
            if (mediaId.equals(item.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static final int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue,
             long queueId) {
        int index = 0;
        for (MediaSession.QueueItem item : queue) {
            if (queueId == item.getQueueId()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static final List<MediaSession.QueueItem> convertToQueue(
            Iterable<MediaMetadata> tracks) {
        List<MediaSession.QueueItem> queue = new ArrayList<>();
        int count = 0;
        for (MediaMetadata track : tracks) {
            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            MediaSession.QueueItem item = new MediaSession.QueueItem(
                    track.getDescription(), count++);
            queue.add(item);
        }
        return queue;

    }

    /**
     * Create a random queue. For simplicity sake, instead of a random queue, we create a
     * queue using the first genre.
     *
     * @param musicProvider
     * @return
     */
    public static final List<MediaSession.QueueItem> getRandomQueue(MusicProvider musicProvider) {
        Iterator<String> genres = musicProvider.getGenres().iterator();
        if (!genres.hasNext()) {
            return new ArrayList<>();
        }
        String genre = genres.next();
        Iterable<MediaMetadata> tracks = musicProvider.getMusicsByGenre(genre);

        return convertToQueue(tracks);
    }



    public static final boolean isIndexPlayable(int index, List<MediaSession.QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }
}
