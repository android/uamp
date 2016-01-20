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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Unit tests for the {@link MediaIDHelper} class. Exercises the helper methods that
 * do MediaID to MusicID conversion and hierarchy (categories) extraction.
 */
@RunWith(JUnit4.class)
public class MediaIDHelperTest {

    @Test
    public void testNormalMediaIDStructure() throws Exception {
        String mediaID = MediaIDHelper.createMediaID("784343", "BY_GENRE", "Classic 70's");
        assertEquals("Classic 70's", MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaID));
        assertEquals("784343", MediaIDHelper.extractMusicIDFromMediaID(mediaID));
    }

    @Test
    public void testSpecialSymbolsMediaIDStructure() throws Exception {
        String mediaID = MediaIDHelper.createMediaID("78A_88|X/3", "BY_GENRE", "Classic 70's");
        assertEquals("Classic 70's", MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaID));
        assertEquals("78A_88|X/3", MediaIDHelper.extractMusicIDFromMediaID(mediaID));
    }

    @Test
    public void testNullMediaIDStructure() throws Exception {
        String mediaID = MediaIDHelper.createMediaID(null, "BY_GENRE", "Classic 70's");
        assertEquals("Classic 70's", MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaID));
        assertNull(MediaIDHelper.extractMusicIDFromMediaID(mediaID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSymbolsInMediaIDStructure() throws Exception {
        fail(MediaIDHelper.createMediaID(null, "BY|GENRE/2", "Classic 70's"));
    }

    @Test
    public void testCreateBrowseCategoryMediaID() throws Exception {
        String browseMediaID = MediaIDHelper.createMediaID(null, "BY_GENRE", "Rock & Roll");
        assertEquals("Rock & Roll", MediaIDHelper.extractBrowseCategoryValueFromMediaID(browseMediaID));
        String[] categories = MediaIDHelper.getHierarchy(browseMediaID);
        assertArrayEquals(categories, new String[]{"BY_GENRE", "Rock & Roll"});
    }

    @Test
    public void testGetParentOfPlayableMediaID() throws Exception {
        String mediaID = MediaIDHelper.createMediaID("23423423", "BY_GENRE", "Rock & Roll");
        String expectedParentID = MediaIDHelper.createMediaID(null, "BY_GENRE", "Rock & Roll");
        assertEquals(expectedParentID, MediaIDHelper.getParentMediaID(mediaID));
    }

    @Test
    public void testGetParentOfBrowsableMediaID() throws Exception {
        String mediaID = MediaIDHelper.createMediaID(null, "BY_GENRE", "Rock & Roll");
        String expectedParentID = MediaIDHelper.createMediaID(null, "BY_GENRE");
        assertEquals(expectedParentID, MediaIDHelper.getParentMediaID(mediaID));
    }

    @Test
    public void testGetParentOfCategoryMediaID() throws Exception {
        assertEquals(
                MediaIDHelper.MEDIA_ID_ROOT,
                MediaIDHelper.getParentMediaID(MediaIDHelper.createMediaID(null, "BY_GENRE")));
    }

    @Test
    public void testGetParentOfRoot() throws Exception {
        assertEquals(
                MediaIDHelper.MEDIA_ID_ROOT,
                MediaIDHelper.getParentMediaID(MediaIDHelper.MEDIA_ID_ROOT));
    }

    @Test(expected=NullPointerException.class)
    public void testGetParentOfNull() throws Exception {
        //noinspection ConstantConditions
        fail(MediaIDHelper.getParentMediaID(null));
    }

}