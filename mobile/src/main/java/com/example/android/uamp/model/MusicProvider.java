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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.text.TextUtilsCompat;
import android.text.TextUtils;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSIC_ALL;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUMS;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSIC_PLAYLISTS;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static com.example.android.uamp.utils.MediaIDHelper.createMediaID;

/**
 * Simple data provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);

    private MusicProviderSource mSource;

    // Categorized caches for music track data:
    private ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByGenre;
    private ConcurrentMap<Long, List<MediaMetadataCompat>> mMusicListByAlbum; // key is album id
    private ConcurrentMap<Long, MediaMetadataCompat> mAlbumMap;
    private final ConcurrentMap<String, MutableMediaMetadata> mMusicListById;

    private final Set<String> mFavoriteTracks;

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    public MusicProvider() {
        this(new RemoteJSONSource());
    }
    public MusicProvider(MusicProviderSource source) {
        mSource = source;
        mMusicListByGenre = new ConcurrentHashMap<>();
        mMusicListById = new ConcurrentHashMap<>();
        mMusicListByAlbum = new ConcurrentHashMap<>();
        mAlbumMap = new ConcurrentHashMap<>();
        // TODO: music by playlist
        mFavoriteTracks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    }

    /**
     * Get an iterator over the list of genres
     *
     * @return genres
     */
    public Iterable<String> getGenres() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mMusicListByGenre.keySet();
    }

    public Iterable<Long> getAlbums() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return mMusicListByAlbum.keySet();
    }

    /**
     * Get an iterator over a shuffled collection of all songs
     */
    public Iterable<MediaMetadataCompat> getShuffledMusic() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> shuffled = new ArrayList<>(mMusicListById.size());
        for (MutableMediaMetadata mutableMetadata: mMusicListById.values()) {
            shuffled.add(mutableMetadata.metadata);
        }
        Collections.shuffle(shuffled);
        return shuffled;
    }

    /**
     * Get music tracks of the given genre
     *
     */
    public Iterable<MediaMetadataCompat> getMusicsByGenre(String genre) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByGenre.containsKey(genre)) {
            return Collections.emptyList();
        }
        return mMusicListByGenre.get(genre);
    }

    public Iterable<MediaMetadataCompat> getMusicsByAlbum(Long albumId) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByAlbum.containsKey(albumId)) {
            return Collections.emptyList();
        }
        return mMusicListByAlbum.get(albumId);
    }

    /**
     * Very basic implementation of a search that filter music tracks with title containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicBySongTitle(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_TITLE, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with album containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicByAlbum(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ALBUM, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with artist containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicByArtist(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ARTIST, query);
    }

    Iterable<MediaMetadataCompat> searchMusic(String metadataField, String query) {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        ArrayList<MediaMetadataCompat> result = new ArrayList<>();
        query = query.toLowerCase(Locale.US);
        for (MutableMediaMetadata track : mMusicListById.values()) {
            if (track.metadata.getString(metadataField).toLowerCase(Locale.US)
                .contains(query)) {
                result.add(track.metadata);
            }
        }
        return result;
    }


    /**
     * Return the MediaMetadataCompat for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    public MediaMetadataCompat getMusic(String musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId).metadata : null;
    }

    public synchronized void updateMusicArt(String musicId, Bitmap albumArt, Bitmap icon) {
        MediaMetadataCompat metadata = getMusic(musicId);
        metadata = new MediaMetadataCompat.Builder(metadata)

                // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                // example, on the lockscreen background when the media session is active.
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)

                // set small version of the album art in the DISPLAY_ICON. This is used on
                // the MediaDescription and thus it should be small to be serialized if
                // necessary
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)

                .build();

        MutableMediaMetadata mutableMetadata = mMusicListById.get(musicId);
        if (mutableMetadata == null) {
            throw new IllegalStateException("Unexpected error: Inconsistent data structures in " +
                    "MusicProvider");
        }

        mutableMetadata.metadata = metadata;
    }

    public void setFavorite(String musicId, boolean favorite) {
        if (favorite) {
            mFavoriteTracks.add(musicId);
        } else {
            mFavoriteTracks.remove(musicId);
        }
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    public boolean isFavorite(String musicId) {
        return mFavoriteTracks.contains(musicId);
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    public void retrieveMediaAsync(final Callback callback) {
        LogHelper.d(TAG, "retrieveMediaAsync called");
        if (mCurrentState == State.INITIALIZED) {
            if (callback != null) {
                // Nothing to do, execute callback immediately
                callback.onMusicCatalogReady(true);
            }
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveMedia();
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }

    private synchronized void buildListsByGenre() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByGenre = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            String genre = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
            List<MediaMetadataCompat> list = newMusicListByGenre.get(genre);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByGenre.put(genre, list);
            }
            list.add(m.metadata);
        }
        mMusicListByGenre = newMusicListByGenre;
    }

    private synchronized void buildListsByAlbum() {
        ConcurrentMap<Long, List<MediaMetadataCompat>> newMusicListByAlbum = new ConcurrentHashMap<>();
        ConcurrentMap<Long, MediaMetadataCompat> newAlbumList = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            if (m.metadata.containsKey(MusicProviderSource.CUSTOM_METADATA_ALBUM_ID)) {
                long aId = m.metadata.getLong(MusicProviderSource.CUSTOM_METADATA_ALBUM_ID);
                String album = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
                List<MediaMetadataCompat> list = newMusicListByAlbum.get(aId);
                if (list == null) {
                    list = new ArrayList<>();
                    newMusicListByAlbum.put(aId, list);
                }
                list.add(m.metadata);

                if (!newAlbumList.containsKey(aId)) {
                    // create something
                    MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                    builder.putLong(MusicProviderSource.CUSTOM_METADATA_ALBUM_ID, aId);
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album);
                    builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI));
                    newAlbumList.put(aId, builder.build());
                }
            }
        }
        mMusicListByAlbum = newMusicListByAlbum;
        mAlbumMap = newAlbumList;
    }

    private synchronized void retrieveMedia() {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;

                Iterator<MediaMetadataCompat> tracks = mSource.iterator();
                while (tracks.hasNext()) {
                    MediaMetadataCompat item = tracks.next();
                    String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    mMusicListById.put(musicId, new MutableMediaMetadata(musicId, item));
                }
                buildListsByGenre();
                buildListsByAlbum();
                mCurrentState = State.INITIALIZED;
            }
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                // Something bad happened, so we reset state to NON_INITIALIZED to allow
                // retries (eg if the network connection is temporary unavailable)
                mCurrentState = State.NON_INITIALIZED;
            }
        }
    }


    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (!MediaIDHelper.isBrowseable(mediaId)) {
            return mediaItems;
        }

        if (MEDIA_ID_ROOT.equals(mediaId)) {
            mediaItems.addAll(createBrowsableMediaItemForRoot(resources));

        } else if (MEDIA_ID_MUSICS_BY_GENRE.equals(mediaId)) {
            for (String genre : getGenres()) {
                mediaItems.add(createBrowsableMediaItemForGenre(genre, resources));
            }
        } else if (MEDIA_ID_MUSICS_BY_ALBUMS.equals(mediaId)) {
            for (Long albumIds: getAlbums()) {
                mediaItems.add(createBrowsableMediaItemForAlbum(mAlbumMap.get(albumIds), resources));
            }
        } else if (MEDIA_ID_MUSIC_PLAYLISTS.equals(mediaId)) {

        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_GENRE)) {
            String genre = MediaIDHelper.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getMusicsByGenre(genre)) {
                mediaItems.add(createMediaItem(metadata, MediaIDHelper.createMediaID(
                        metadata.getDescription().getMediaId(), MEDIA_ID_MUSICS_BY_GENRE, genre)));
            }
        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_ALBUMS)) {
            Long albumId = Long.valueOf(MediaIDHelper.getHierarchy(mediaId)[1]);

            for (MediaMetadataCompat metadata: getMusicsByAlbum(albumId)) {
                String hierarchicalMediaId =  MediaIDHelper.createMediaID(
                        metadata.getDescription().getMediaId(), MEDIA_ID_MUSICS_BY_ALBUMS, String.valueOf(albumId));
                mediaItems.add(createMediaItem(metadata, hierarchicalMediaId));
            }

        } else if (mediaId.startsWith(MEDIA_ID_MUSIC_PLAYLISTS)) {

        } else if (MEDIA_ID_MUSIC_ALL.equals(mediaId)) {

            for (MutableMediaMetadata m : mMusicListById.values()){
                String hierarchicalMediaId =  MediaIDHelper.createMediaID(
                        m.metadata.getDescription().getMediaId(), MEDIA_ID_MUSIC_ALL, "all_music"); // hard coded to include all music
                mediaItems.add(createMediaItem(m.metadata, hierarchicalMediaId));
            }
        } else {
            LogHelper.w(TAG, "Skipping unmatched mediaId: ", mediaId);
        }
        return mediaItems;
    }

    private List<MediaBrowserCompat.MediaItem> createBrowsableMediaItemForRoot(Resources resources) {
        // TODO fix category thumbnails
        MediaBrowserCompat.MediaItem genre = createRootElement(resources, MEDIA_ID_MUSICS_BY_GENRE,
                resources.getString(R.string.browse_genres),
                resources.getString(R.string.browse_genre_subtitle),
                Uri.parse("android.resource://com.example.android.uamp/drawable/ic_by_genre"));

        MediaBrowserCompat.MediaItem album = createRootElement(resources, MEDIA_ID_MUSICS_BY_ALBUMS,
                resources.getString(R.string.browse_albums),
                resources.getString(R.string.browse_albums_subtitle),
                Uri.parse("android.resource://com.example.android.uamp/drawable/ic_by_genre"));

        MediaBrowserCompat.MediaItem playlist = createRootElement(resources, MEDIA_ID_MUSIC_PLAYLISTS,
                resources.getString(R.string.browse_playlist),
                resources.getString(R.string.browse_playlist_subtitle),
                Uri.parse("android.resource://com.example.android.uamp/drawable/ic_by_genre"));

        MediaBrowserCompat.MediaItem all = createRootElement(resources, MEDIA_ID_MUSIC_ALL,
                resources.getString(R.string.browse_tracks),
                resources.getString(R.string.browse_tracks_subtitle),
                Uri.parse("android.resource://com.example.android.uamp/drawable/ic_by_genre"));

        return Arrays.asList(playlist, album, genre, all);
    }

    private MediaBrowserCompat.MediaItem createRootElement(Resources resources, String mediaId, String title, String subtitle, Uri iconUri) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setIconUri(iconUri)
                .build();
        return new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForAlbum(MediaMetadataCompat album, Resources resources) {
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_ALBUMS, String.valueOf(album.getLong(MusicProviderSource.CUSTOM_METADATA_ALBUM_ID))))
                .setTitle(album.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
                .setSubtitle(resources.getString(
                        R.string.browse_musics_by_album_subtitle, album.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)));

        // TODO: figure out why this doesn't really load a thing
        if (!TextUtils.isEmpty(album.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))) {
            builder.setIconUri(Uri.parse(album.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)));
        }

        return new MediaBrowserCompat.MediaItem(builder.build(),MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForPlaylist(String playlistId, Resources resources) {
        return null;
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForGenre(String genre,
                                                                    Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_GENRE, genre))
                .setTitle(genre)
                .setSubtitle(resources.getString(
                        R.string.browse_musics_by_genre_subtitle, genre))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata, String hierarchyAwareMediaID) {
        // Since mediaMetadata fields are immutable, we need to create a copy, so we
        // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
        // when we get a onPlayFromMusicID call, so we can create the proper queue based
        // on where the music was selected from (by artist, by genre, random, etc)
        String genre = metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
        MediaMetadataCompat copy = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build();
        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

    }

}
