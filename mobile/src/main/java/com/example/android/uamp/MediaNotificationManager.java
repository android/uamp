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

package com.example.android.uamp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

import com.example.android.uamp.ui.MusicPlayerActivity;
import com.example.android.uamp.utils.LogHelper;

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
public class MediaNotificationManager extends BroadcastReceiver {
    private static final String TAG = LogHelper.makeLogTag(MediaNotificationManager.class);

    private static final int NOTIFICATION_ID = 412;

    public static final String ACTION_PAUSE = "com.example.android.uamp.pause";
    public static final String ACTION_PLAY = "com.example.android.uamp.play";
    public static final String ACTION_PREV = "com.example.android.uamp.prev";
    public static final String ACTION_NEXT = "com.example.android.uamp.next";

    private final MusicService mService;
    private MediaSession.Token mSessionToken;
    private MediaController mController;
    private MediaController.TransportControls mTransportControls;

    private PlaybackState mPlaybackState;
    private MediaMetadata mMetadata;

    private Notification.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private Notification.Action mPlayPauseAction;

    private PendingIntent mPauseIntent, mPlayIntent, mPreviousIntent, mNextIntent, mContentIntent;

    private int mNotificationColor;

    private boolean mStarted = false;

    public MediaNotificationManager(MusicService service) {
        mService = service;
        updateSessionToken();

        mNotificationColor = getNotificationColor();

        mNotificationManager = (NotificationManager) mService
                .getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = mService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        Intent openUI = new Intent(mService, MusicPlayerActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContentIntent = PendingIntent.getActivity(mService, 100, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();
    }

    protected int getNotificationColor() {
        int notificationColor = 0;
        String packageName = mService.getPackageName();
        try {
            Context packageContext = mService.createPackageContext(packageName, 0);
            ApplicationInfo applicationInfo =
                    mService.getPackageManager().getApplicationInfo(packageName, 0);
            packageContext.setTheme(applicationInfo.theme);
            Resources.Theme theme = packageContext.getTheme();
            TypedArray ta = theme.obtainStyledAttributes(
                    new int[] {android.R.attr.colorPrimary});
            notificationColor = ta.getColor(0, Color.DKGRAY);
            ta.recycle();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return notificationColor;
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!mStarted) {
            mMetadata = mController.getMetadata();
            mPlaybackState = mController.getPlaybackState();

            mController.registerCallback(mCb);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_NEXT);
            filter.addAction(ACTION_PAUSE);
            filter.addAction(ACTION_PLAY);
            filter.addAction(ACTION_PREV);
            mService.registerReceiver(this, filter);

            mStarted = true;
            // The notification must be updated after setting started to true
            updateNotificationMetadata();
            mService.startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        mStarted = false;
        mController.unregisterCallback(mCb);
        try {
            mNotificationManager.cancel(NOTIFICATION_ID);
            mService.unregisterReceiver(this);
        } catch (IllegalArgumentException ex) {
            // ignore if the receiver is not registered.
        }
        mService.stopForeground(true);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        LogHelper.d(TAG, "Received intent with action " + action);
        if (ACTION_PAUSE.equals(action)) {
            mTransportControls.pause();
        } else if (ACTION_PLAY.equals(action)) {
            mTransportControls.play();
        } else if (ACTION_NEXT.equals(action)) {
            mTransportControls.skipToNext();
        } else if (ACTION_PREV.equals(action)) {
            mTransportControls.skipToPrevious();
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() {
        MediaSession.Token freshToken = mService.getSessionToken();
        if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCb);
            }
            mSessionToken = freshToken;
            mController = new MediaController(mService, mSessionToken);
            mTransportControls = mController.getTransportControls();
            if (mStarted) {
                mController.registerCallback(mCb);
            }
        }
    }

    private final MediaController.Callback mCb = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            mPlaybackState = state;
            LogHelper.d(TAG, "Received new playback state", state);
            updateNotificationPlaybackState();
            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            mMetadata = metadata;
            LogHelper.d(TAG, "Received new metadata ", metadata);
            updateNotificationMetadata();
            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            LogHelper.d(TAG, "Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }
    };

    private void updateNotificationMetadata() {
        LogHelper.d(TAG, "updateNotificationMetadata. mMetadata=" + mMetadata);
        if (mMetadata == null || mPlaybackState == null) {
            return;
        }

        updatePlayPauseAction();

        mNotificationBuilder = new Notification.Builder(mService);
        int playPauseActionIndex = 0;

        // If skip to previous action is enabled
        if ((mPlaybackState.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) {
            mNotificationBuilder
                    .addAction(R.drawable.ic_skip_previous_white_24dp,
                            mService.getString(R.string.label_previous), mPreviousIntent);
            playPauseActionIndex = 1;
        }

        mNotificationBuilder.addAction(mPlayPauseAction);

        // If skip to next action is enabled
        if ((mPlaybackState.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) {
            mNotificationBuilder.addAction(R.drawable.ic_skip_next_white_24dp,
                    mService.getString(R.string.label_next), mNextIntent);
        }

        MediaDescription description = mMetadata.getDescription();

        String fetchArtUrl = null;
        Bitmap art = description.getIconBitmap();
        if (art == null && description.getIconUri() != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            String artUrl = description.getIconUri().toString();
            art = mService.getAlbumArtCache().get(artUrl);
            if (art == null) {
                fetchArtUrl = artUrl;
                // use a placeholder art while the remote art is being downloaded
                art = BitmapFactory.decodeResource(mService.getResources(), R.drawable.ic_default_art);
            }
        }

        mNotificationBuilder
                .setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(playPauseActionIndex)  // only show play/pause in compact view
                        .setMediaSession(mSessionToken))
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentIntent(mContentIntent)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(art);

        updateNotificationPlaybackState();

        if (fetchArtUrl != null) {
            fetchBitmapFromURLAsync(fetchArtUrl);
        }
    }

    private void updatePlayPauseAction() {
        LogHelper.d(TAG, "updatePlayPauseAction");
        String label;
        int icon;
        PendingIntent intent;
        if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING) {
            label = mService.getString(R.string.label_pause);
            icon = R.drawable.ic_pause_white_24dp;
            intent = mPauseIntent;
        } else {
            label = mService.getString(R.string.label_play);
            icon = R.drawable.ic_play_arrow_white_24dp;
            intent = mPlayIntent;
        }
        if (mPlayPauseAction == null) {
            mPlayPauseAction = new Notification.Action(icon, label, intent);
        } else {
            mPlayPauseAction.icon = icon;
            mPlayPauseAction.title = label;
            mPlayPauseAction.actionIntent = intent;
        }
    }

    private void updateNotificationPlaybackState() {
        LogHelper.d(TAG, "updateNotificationPlaybackState. mPlaybackState=" + mPlaybackState);
        if (mPlaybackState == null || !mStarted) {
            LogHelper.d(TAG, "updateNotificationPlaybackState. cancelling notification!");
            mService.stopForeground(true);
            return;
        }
        if (mNotificationBuilder == null) {
            LogHelper.d(TAG, "updateNotificationPlaybackState. there is no notificationBuilder. Ignoring request to update state!");
            return;
        }
        if (mPlaybackState.getState() == PlaybackState.STATE_PLAYING
                && mPlaybackState.getPosition() >= 0) {
            LogHelper.d(TAG, "updateNotificationPlaybackState. updating playback position to ",
                    (System.currentTimeMillis() - mPlaybackState.getPosition()) / 1000, " seconds");
            mNotificationBuilder
                    .setWhen(System.currentTimeMillis() - mPlaybackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            LogHelper.d(TAG, "updateNotificationPlaybackState. hiding playback position");
            mNotificationBuilder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        updatePlayPauseAction();

        // Make sure that the notification can be dismissed by the user when we are not playing:
        mNotificationBuilder.setOngoing(mPlaybackState.getState() == PlaybackState.STATE_PLAYING);

    }

    private void fetchBitmapFromURLAsync(final String source) {
        mService.getAlbumArtCache().fetch(source, new AlbumArtCache.FetchListener() {
            @Override
            public void onFetched(String artUrl, Bitmap bitmap) {
                if (bitmap != null && mMetadata != null &&
                        mNotificationBuilder != null && mMetadata.getDescription() != null &&
                        !source.equals(mMetadata.getDescription().getIconUri())) {
                    // If the media is still the same, update the notification:
                    LogHelper.d(TAG, "fetchBitmapFromURLAsync: set bitmap to ", source);
                    mNotificationBuilder.setLargeIcon(bitmap);
                    mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                }
            }
        });
    }
}
