package com.example.android.uamp.model;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import static com.example.android.uamp.utils.MediaIDHelper.CATEGORY_SEPARATOR;
import static com.example.android.uamp.utils.MediaIDHelper.LEAF_SEPARATOR;

public class LocalSource implements MusicProviderSource {
    private static final String TAG = "LocalSource";
    public static final String NO_GENRE = "no-genre";

    private enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    private ContentResolver mContentResolver;

    private final LinkedHashMap<String, MediaMetadataCompat> mItems;
    private final HashMap<Long, Long> mediaIdToGenreMap;
    private final HashMap<Long, String> genreNameMap;
    private final HashMap<Long, LinkedList<Long>> playlistIdMap;

    public LocalSource(ContentResolver resolver) {
        this.mContentResolver = resolver;

        mItems = new LinkedHashMap<>();
        mediaIdToGenreMap = new HashMap<>();
        playlistIdMap = new HashMap<>();
        genreNameMap = new HashMap<>();
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    // work around the strange media id construction for ump
    private static String filterGenreName(String in) {
        in = in.replaceAll("/", "_");
        in = in.replaceAll("\\|", ".");
        return in;
    }

    private void loadGenreToIdMap() {
        Uri uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;

        Cursor cur = mContentResolver.query(uri, null, null, null, null);

        if (cur == null || !cur.moveToFirst()) {
            Log.d(TAG, "prepGenre: no genre found?");
            return;
        }

        int nameColumn = cur.getColumnIndex(MediaStore.Audio.Genres.NAME);
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Genres._ID);

        do {
            long curId = cur.getLong(idColumn);
            String name = cur.getString(nameColumn);
            Log.d(TAG, "genre: " + name + "[" + curId + "]");

            genreNameMap.put(curId, filterGenreName(name));
        } while (cur.moveToNext());

        cur.close();

        for(Long id : genreNameMap.keySet()) {
            Uri memberUri = MediaStore.Audio.Genres.Members.getContentUri("external", id);
            Cursor c = mContentResolver.query(memberUri, null, null, null, null);

            if (c == null || !c.moveToFirst()) {
                continue;
            }

            int audioIdColumn = c.getColumnIndex(MediaStore.Audio.Genres.Members.AUDIO_ID);

            do {
                long aid = c.getLong(audioIdColumn);
                mediaIdToGenreMap.put(aid, id);
            } while (c.moveToNext());

            c.close();
        }
    }

    private void loadPlaylistIdMap() {
        
    }

    public void prepare() {
        mCurrentState = State.INITIALIZING;

        loadGenreToIdMap();

        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.i(TAG, "Querying media...");
        Log.i(TAG, "URI: " + uri.toString());

        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        Cursor cur = mContentResolver.query(uri, null,
                MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);
        Log.i(TAG, "Query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

        if (cur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null :-(");
            return;
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            Log.e(TAG, "Failed to move cursor to first row (no query results).");
            return;
        }

        Log.i(TAG, "Listing...");

        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);

        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

        int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

//        Log.i(TAG, "Title column index: " + String.valueOf(titleColumn));
//        Log.i(TAG, "ID column index: " + String.valueOf(titleColumn));

        // add each song to mItems
        do {
//            Log.i(TAG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn));

            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
            long mediaId = cur.getLong(idColumn);
            builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(mediaId))
//                    .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, String.valueOf(ContentUris.withAppendedId(
//                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)))
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, String.valueOf(ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)))
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, cur.getString(titleColumn))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, cur.getString(artistColumn))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, cur.getString(albumColumn))
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, getGenre(mediaId))
                    .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, cur.getString(dataColumn))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, cur.getLong(durationColumn));

            mItems.put(String.valueOf(mediaId), builder.build());

        } while (cur.moveToNext());

        Log.i(TAG, "Done querying media. MusicRetriever is ready.");

        cur.close();

        mCurrentState = State.INITIALIZED;

    }

    private String getGenre(long audioId) {
        if (mediaIdToGenreMap.containsKey(audioId)) {
            Long gid = mediaIdToGenreMap.get(audioId);
            if (genreNameMap.containsKey(gid)) {
                return genreNameMap.get(gid);
            }
        }
        return NO_GENRE;
    }

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        if (isInitialized()) {
            return mItems.values().iterator();
        }

        try {
            prepare();
        } catch (SecurityException e) {
            mCurrentState = State.NON_INITIALIZED;
        }
        return mItems.values().iterator();
    }
}
