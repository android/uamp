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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;

import java.util.Arrays;

/**
 * Utility class to help on queue related tasks.
 */
public class MediaIDHelper {

    // Media IDs used on browseable items of MediaBrowser
    public static final String MEDIA_ID_EMPTY_ROOT = "__EMPTY_ROOT__";
    public static final String MEDIA_ID_ROOT = "__ROOT__";
    public static final String MEDIA_ID_MUSICS_BY_GENRE = "__BY_GENRE__";
    public static final String MEDIA_ID_MUSICS_BY_SEARCH = "__BY_SEARCH__";
    public static final String MEDIA_ID_MUSIC_PLAYLISTS = "__MUSIC_PLAYLISTS__";
    public static final String MEDIA_ID_MUSICS_BY_ALBUMS = "__BY_ALBUMS__";
    public static final String MEDIA_ID_MUSIC_ALL = "__ALL__";

    public static final char CATEGORY_SEPARATOR = '/';
    public static final char LEAF_SEPARATOR = '|';

    /**
     * Create a String value that represents a playable or a browsable media.
     *
     * Encode the media browseable categories, if any, and the unique music ID, if any,
     * into a single String mediaID.
     *
     * MediaIDs are of the form <categoryType>/<categoryValue>|<musicUniqueId>, to make it easy
     * to find the category (like genre) that a music was selected from, so we
     * can correctly build the playing queue. This is specially useful when
     * one music can appear in more than one list, like "by genre -> genre_1"
     * and "by artist -> artist_1".

     * @param musicID Unique music ID for playable items, or null for browseable items.
     * @param categories hierarchy of categories representing this item's browsing parents
     * @return a hierarchy-aware media ID
     */
    public static String createMediaID(String musicID, String... categories) {
        StringBuilder sb = new StringBuilder();
        if (categories != null) {
            for (int i=0; i < categories.length; i++) {
                if (!isValidCategory(categories[i])) {
                    throw new IllegalArgumentException("Invalid category: " + categories[i]);
                }
                sb.append(categories[i]);
                if (i < categories.length - 1) {
                    sb.append(CATEGORY_SEPARATOR);
                }
            }
        }
        if (musicID != null) {
            sb.append(LEAF_SEPARATOR).append(musicID);
        }
        return sb.toString();
    }

    private static boolean isValidCategory(String category) {
        return category == null ||
                (
                    category.indexOf(CATEGORY_SEPARATOR) < 0 &&
                    category.indexOf(LEAF_SEPARATOR) < 0
                );
    }

    /**
     * Extracts unique musicID from the mediaID. mediaID is, by this sample's convention, a
     * concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and unique
     * musicID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param mediaID that contains the musicID
     * @return musicID
     */
    public static String extractMusicIDFromMediaID(@NonNull String mediaID) {
        int pos = mediaID.indexOf(LEAF_SEPARATOR);
        if (pos >= 0) {
            return mediaID.substring(pos+1);
        }
        return null;
    }

    /**
     * Extracts category and categoryValue from the mediaID. mediaID is, by this sample's
     * convention, a concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and
     * mediaID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param mediaID that contains a category and categoryValue.
     */
    public static @NonNull String[] getHierarchy(@NonNull String mediaID) {
        int pos = mediaID.indexOf(LEAF_SEPARATOR);
        if (pos >= 0) {
            mediaID = mediaID.substring(0, pos);
        }
        return mediaID.split(String.valueOf(CATEGORY_SEPARATOR));
    }

    public static String extractBrowseCategoryValueFromMediaID(@NonNull String mediaID) {
        String[] hierarchy = getHierarchy(mediaID);
        if (hierarchy.length == 2) {
            return hierarchy[1];
        }
        return null;
    }

    public static boolean isBrowseable(@NonNull String mediaID) {
        return mediaID.indexOf(LEAF_SEPARATOR) < 0;
    }

    public static String getParentMediaID(@NonNull String mediaID) {
        String[] hierarchy = getHierarchy(mediaID);
        if (!isBrowseable(mediaID)) {
            return createMediaID(null, hierarchy);
        }
        if (hierarchy.length <= 1) {
            return MEDIA_ID_ROOT;
        }
        String[] parentHierarchy = Arrays.copyOf(hierarchy, hierarchy.length-1);
        return createMediaID(null, parentHierarchy);
    }

    /**
     * Determine if media item is playing (matches the currently playing media item).
     *
     * @param context for retrieving the {@link MediaControllerCompat}
     * @param mediaItem to compare to currently playing {@link MediaBrowserCompat.MediaItem}
     * @return boolean indicating whether media item matches currently playing media item
     */
    public static boolean isMediaItemPlaying(Context context,
                                             MediaBrowserCompat.MediaItem mediaItem) {
        // Media item is considered to be playing or paused based on the controller's current
        // media id
        MediaControllerCompat controller = MediaControllerCompat.getMediaController((FragmentActivity) context);
        if (controller != null && controller.getMetadata() != null) {
            String currentPlayingMediaId = controller.getMetadata().getDescription()
                    .getMediaId();
            String itemMusicId = MediaIDHelper.extractMusicIDFromMediaID(
                    mediaItem.getDescription().getMediaId());
            if (currentPlayingMediaId != null
                    && TextUtils.equals(currentPlayingMediaId, itemMusicId)) {
                return true;
            }
        }
        return false;
    }
}
