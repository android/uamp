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

import android.support.annotation.NonNull;

import java.util.Arrays;

/**
 * Utility class to help on queue related tasks.
 */
public class MediaIDHelper {

    // Media IDs used on browseable items of MediaBrowser
    public static final String MEDIA_ID_ROOT = "__ROOT__";
    public static final String MEDIA_ID_MUSICS_BY_GENRE = "__BY_GENRE__";
    public static final String MEDIA_ID_MUSICS_BY_SEARCH = "__BY_SEARCH__";

    private static final char CATEGORY_SEPARATOR = '/';
    private static final char LEAF_SEPARATOR = '|';

    public static String createMediaID(String musicID, String... categories) {
        // MediaIDs are of the form <categoryType>/<categoryValue>|<musicUniqueId>, to make it easy
        // to find the category (like genre) that a music was selected from, so we
        // can correctly build the playing queue. This is specially useful when
        // one music can appear in more than one list, like "by genre -> genre_1"
        // and "by artist -> artist_1".
        StringBuilder sb = new StringBuilder();
        if (categories != null && categories.length > 0) {
            sb.append(categories[0]);
            for (int i=1; i < categories.length; i++) {
                sb.append(CATEGORY_SEPARATOR).append(categories[i]);
            }
        }
        if (musicID != null) {
            sb.append(LEAF_SEPARATOR).append(musicID);
        }
        return sb.toString();
    }

    public static String createBrowseCategoryMediaID(String categoryType, String categoryValue) {
        return categoryType + CATEGORY_SEPARATOR + categoryValue;
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
    public static String extractMusicIDFromMediaID(String mediaID) {
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
    public static @NonNull String[] getHierarchy(String mediaID) {
        int pos = mediaID.indexOf(LEAF_SEPARATOR);
        if (pos >= 0) {
            mediaID = mediaID.substring(0, pos);
        }
        return mediaID.split(String.valueOf(CATEGORY_SEPARATOR));
    }

    public static String extractBrowseCategoryValueFromMediaID(String mediaID) {
        String[] hierarchy = getHierarchy(mediaID);
        if (hierarchy.length == 2) {
            return hierarchy[1];
        }
        return null;
    }

    private static boolean isBrowseable(String mediaID) {
        return mediaID.indexOf(LEAF_SEPARATOR) < 0;
    }

    public static String getParentMediaID(String mediaID) {
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
}
