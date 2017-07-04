package com.example.android.uamp.model;


import android.content.ContentResolver;
import android.support.v4.media.MediaMetadataCompat;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class LocalContentProviderSource implements MusicProviderSource {

    private enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    private ContentResolver mResolver;

    private final LinkedHashMap<String, MediaMetadataCompat> mItems;

    public LocalContentProviderSource(ContentResolver resolver) {
        this.mResolver = resolver;

        mItems = new LinkedHashMap<>();
    }

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        return mItems.values().iterator();
    }
}
