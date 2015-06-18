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
import android.os.Bundle;

import com.example.android.uamp.VoiceSearchParams;
import com.example.android.uamp.model.MusicProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_SEARCH;

/**
 * Utility class to help on queue related tasks.
 */
public class QueueHelper {

    private static final String TAG = LogHelper.makeLogTag(QueueHelper.class);

    public static List<MediaSession.QueueItem> getPlayingQueue(String mediaId,
            MusicProvider musicProvider) {

        // extract the browsing hierarchy from the media ID:
        String[] hierarchy = MediaIDHelper.getHierarchy(mediaId);

        if (hierarchy.length != 2) {
            LogHelper.e(TAG, "Could not build a playing queue for this mediaId: ", mediaId);
            return null;
        }

        String categoryType = hierarchy[0];
        String categoryValue = hierarchy[1];
        LogHelper.d(TAG, "Creating playing queue for ", categoryType, ",  ", categoryValue);

        Iterable<MediaMetadata> tracks = null;
        // This sample only supports genre and by_search category types.
        if (categoryType.equals(MEDIA_ID_MUSICS_BY_GENRE)) {
            tracks = musicProvider.getMusicsByGenre(categoryValue);
        } else if (categoryType.equals(MEDIA_ID_MUSICS_BY_SEARCH)) {
            tracks = musicProvider.searchMusicBySongTitle(categoryValue);
        }

        if (tracks == null) {
            LogHelper.e(TAG, "Unrecognized category type: ", categoryType, " for media ", mediaId);
            return null;
        }

        return convertToQueue(tracks, hierarchy[0], hierarchy[1]);
    }

    public static List<MediaSession.QueueItem> getPlayingQueueFromSearch(String query,
            Bundle queryParams, MusicProvider musicProvider) {

        LogHelper.d(TAG, "Creating playing queue for musics from search: ", query,
            " params=", queryParams);

        VoiceSearchParams params = new VoiceSearchParams(query, queryParams);

        LogHelper.d(TAG, "VoiceSearchParams: ", params);

        if (params.isAny) {
            // If isAny is true, we will play anything. This is app-dependent, and can be,
            // for example, favorite playlists, "I'm feeling lucky", most recent, etc.
            return getRandomQueue(musicProvider);
        }

        Iterable<MediaMetadata> result = null;
        if (params.isAlbumFocus) {
            result = musicProvider.searchMusicByAlbum(params.album);
        } else if (params.isGenreFocus) {
            result = musicProvider.getMusicsByGenre(params.genre);
        } else if (params.isArtistFocus) {
            result = musicProvider.searchMusicByArtist(params.artist);
        } else if (params.isSongFocus) {
            result = musicProvider.searchMusicBySongTitle(params.song);
        }

        // If there was no results using media focus parameter, we do an unstructured query.
        // This is useful when the user is searching for something that looks like an artist
        // to Google, for example, but is not. For example, a user searching for Madonna on
        // a PodCast application wouldn't get results if we only looked at the
        // Artist (podcast author). Then, we can instead do an unstructured search.
        if (params.isUnstructured || result == null || !result.iterator().hasNext()) {
            // To keep it simple for this example, we do unstructured searches on the
            // song title only. A real world application could search on other fields as well.
            result = musicProvider.searchMusicBySongTitle(query);
        }

        return convertToQueue(result, MEDIA_ID_MUSICS_BY_SEARCH, query);
    }


    public static int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue,
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

    public static int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue,
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

    private static List<MediaSession.QueueItem> convertToQueue(
            Iterable<MediaMetadata> tracks, String... categories) {
        List<MediaSession.QueueItem> queue = new ArrayList<>();
        int count = 0;
        for (MediaMetadata track : tracks) {

            // We create a hierarchy-aware mediaID, so we know what the queue is about by looking
            // at the QueueItem media IDs.
            String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                    track.getDescription().getMediaId(), categories);

            MediaMetadata trackCopy = new MediaMetadata.Builder(track)
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                    .build();

            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            MediaSession.QueueItem item = new MediaSession.QueueItem(
                    trackCopy.getDescription(), count++);
            queue.add(item);
        }
        return queue;

    }

    /**
     * Create a random queue.
     *
     * @param musicProvider the provider used for fetching music.
     * @return list containing {@link MediaSession.QueueItem}'s
     */
    public static List<MediaSession.QueueItem> getRandomQueue(MusicProvider musicProvider) {
        List<MediaMetadata> result = new ArrayList<>();

        for (String genre: musicProvider.getGenres()) {
            Iterable<MediaMetadata> tracks = musicProvider.getMusicsByGenre(genre);
            for (MediaMetadata track: tracks) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    result.add(track);
                }
            }
        }
        LogHelper.d(TAG, "getRandomQueue: result.size=", result.size());

        Collections.shuffle(result);

        return convertToQueue(result, MEDIA_ID_MUSICS_BY_SEARCH, "random");
    }

    public static boolean isIndexPlayable(int index, List<MediaSession.QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }
}
