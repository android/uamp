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

/**
 * Utility class to help on queue related tasks.
 */
public class MediaIDHelper {

    private static final String TAG = LogHelper.makeLogTag(MediaIDHelper.class);

    // Media IDs used on browseable items of MediaBrowser
    public static final String MEDIA_ID_ROOT = "__ROOT__";
    public static final String MEDIA_ID_MUSICS_BY_GENRE = "__BY_GENRE__";

    public static final String createTrackMediaID(String categoryType, String categoryValue,
              MediaMetadata track) {
        // MediaIDs are of the form <categoryType>/<categoryValue>|<musicUniqueId>, to make it easy to
        // find the category (like genre) that a music was selected from, so we
        // can correctly build the playing queue. This is specially useful when
        // one music can appear in more than one list, like "by genre -> genre_1"
        // and "by artist -> artist_1".
        return categoryType + "/" + categoryValue + "|" +
                track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
    }

    public static final String createBrowseCategoryMediaID(String categoryType, String categoryValue) {
        return categoryType + "/" + categoryValue;
    }

    /**
     * Extracts unique musicID from the mediaID. mediaID is, by this sample's convention, a
     * concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and unique
     * musicID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param musicID
     * @return
     */
    public static final String extractMusicIDFromMediaID(String musicID) {
        String[] segments = musicID.split("\\|", 2);
        return segments.length == 2 ? segments[1] : null;
    }

    /**
     * Extracts category and categoryValue from the mediaID. mediaID is, by this sample's
     * convention, a concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and
     * mediaID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param mediaID
     * @return
     */
    public static final String[] extractBrowseCategoryFromMediaID(String mediaID) {
        if (mediaID.indexOf('|') >= 0) {
            mediaID = mediaID.split("\\|")[0];
        }
        if (mediaID.indexOf('/') == 0) {
            return new String[]{mediaID, null};
        } else {
            return mediaID.split("/", 2);
        }
    }

    public static final String extractBrowseCategoryValueFromMediaID(String mediaID) {
        String[] categoryAndValue = extractBrowseCategoryFromMediaID(mediaID);
        if (categoryAndValue != null && categoryAndValue.length == 2) {
            return categoryAndValue[1];
        }
        return null;
    }
}
