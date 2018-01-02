/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.media.audiofocus;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.media.AudioAttributesCompat;
import android.util.Log;

/**
 * A class to help request and abandon audio focus, with proper handling of API 26+
 * audio focus changes.
 */
public class AudioFocusHelper {
    private static final String TAG = "AudioFocusHelper";

    private final AudioFocusHelperImpl mImpl;

    /**
     * Creates an AudioFocusHelper given a {@see Context}.
     * <p>
     * This does not request audio focus.
     *
     * @param context The current context.
     */
    public AudioFocusHelper(@NonNull final Context context) {
        final AudioManager audioManager = (AudioManager) context
                .getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mImpl = new AudioFocusHelperImplApi26(audioManager);
        } else {
            mImpl = new AudioFocusHelperImplBase(audioManager);
        }
    }

    /**
     * Builds an {@link OnAudioFocusChangeListener} to control an
     * {@link AudioFocusAwarePlayer} in response to audio focus changes.
     * <p>
     * This function is intended to be used in conjuction with an {@link AudioFocusRequestCompat}
     * as follows:
     * <code>
     * AudioFocusRequestCompat focusRequest =
     *     new AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN)
     *         .setOnAudioFocusChangeListener(audioFocusHelper.createListenerForPlayer(player))
     *         // etc...
     *         .build();
     * </code>
     *
     * @param player The player that will respond to audio focus changes.
     * @return An {@link OnAudioFocusChangeListener} to control the player.
     */
    public OnAudioFocusChangeListener createListenerForPlayer(
            @NonNull AudioFocusAwarePlayer player) {

        return new DefaultAudioFocusListener(mImpl, player);
    }

    /**
     * Requests audio focus for the player.
     *
     * @param audioFocusRequestCompat The audio focus request to perform.
     * @return {@code true} if audio focus was granted, {@code false} otherwise.
     */
    public boolean requestAudioFocus(AudioFocusRequestCompat audioFocusRequestCompat) {
        return mImpl.requestAudioFocus(audioFocusRequestCompat);
    }

    /**
     * Abandons audio focus.
     *
     * @param audioFocusRequestCompat The audio focus request to abandon.
     */
    public void abandonAudioFocus(AudioFocusRequestCompat audioFocusRequestCompat) {
        mImpl.abandonAudioFocus();
    }

    interface AudioFocusHelperImpl {
        boolean requestAudioFocus(AudioFocusRequestCompat audioFocusRequestCompat);

        void abandonAudioFocus();

        boolean willPauseWhenDucked();
    }

    private static class AudioFocusHelperImplBase implements AudioFocusHelperImpl {
        final AudioManager mAudioManager;
        AudioFocusRequestCompat mAudioFocusRequestCompat;

        AudioFocusHelperImplBase(AudioManager audioManager) {
            mAudioManager = audioManager;
        }

        @Override
        public boolean requestAudioFocus(AudioFocusRequestCompat audioFocusRequestCompat) {
            // Save the focus request.
            mAudioFocusRequestCompat = audioFocusRequestCompat;

            // Check for possible problems...
            if (audioFocusRequestCompat.acceptsDelayedFocusGain()) {
                final String message = "Cannot request delayed focus on API " +
                        Build.VERSION.SDK_INT;

                // Make an exception to allow the developer to more easily find this code path.
                @SuppressWarnings("ThrowableNotThrown")
                final Throwable stackTrace = new UnsupportedOperationException(message)
                        .fillInStackTrace();
                Log.w(TAG, "Cannot request delayed focus", stackTrace);
            }

            final OnAudioFocusChangeListener listener =
                    mAudioFocusRequestCompat.getOnAudioFocusChangeListener();
            final int streamType =
                    mAudioFocusRequestCompat.getAudioAttributesCompat().getLegacyStreamType();
            final int focusGain = mAudioFocusRequestCompat.getFocusGain();

            return mAudioManager.requestAudioFocus(listener, streamType, focusGain) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }

        @Override
        public void abandonAudioFocus() {
            if (mAudioFocusRequestCompat == null) {
                return;
            }

            mAudioManager.abandonAudioFocus(
                    mAudioFocusRequestCompat.getOnAudioFocusChangeListener());
        }

        @Override
        public boolean willPauseWhenDucked() {
            if (mAudioFocusRequestCompat == null) {
                return false;
            }

            final AudioAttributesCompat audioAttributes =
                    mAudioFocusRequestCompat.getAudioAttributesCompat();

            final boolean pauseWhenDucked = mAudioFocusRequestCompat.willPauseWhenDucked();
            final boolean isSpeech = (audioAttributes != null) &&
                    audioAttributes.getContentType() == AudioAttributesCompat.CONTENT_TYPE_SPEECH;
            return pauseWhenDucked || isSpeech;
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static class AudioFocusHelperImplApi26 extends AudioFocusHelperImplBase {
        private AudioFocusRequest mAudioFocusRequest;

        AudioFocusHelperImplApi26(AudioManager audioManager) {
            super(audioManager);
        }

        @Override
        public boolean requestAudioFocus(AudioFocusRequestCompat audioFocusRequestCompat) {
            // Save and unwrap the compat object.
            mAudioFocusRequestCompat = audioFocusRequestCompat;
            mAudioFocusRequest = audioFocusRequestCompat.getAudioFocusRequest();

            return mAudioManager.requestAudioFocus(mAudioFocusRequest) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }

        @Override
        public void abandonAudioFocus() {
            mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
        }
    }

    /**
     * Implementation of an Android Oreo inspired {@link OnAudioFocusChangeListener}.
     */
    private static class DefaultAudioFocusListener
            implements OnAudioFocusChangeListener {

        private static final float MEDIA_VOLUME_DEFAULT = 1.0f;
        private static final float MEDIA_VOLUME_DUCK = 0.2f;

        private final AudioFocusHelperImpl mImpl;
        private final AudioFocusAwarePlayer mPlayer;

        private boolean mResumeOnFocusGain = false;

        private DefaultAudioFocusListener(AudioFocusHelperImpl impl, AudioFocusAwarePlayer player) {
            mImpl = impl;
            mPlayer = player;
        }

        private AudioFocusAwarePlayer getPlayer() {
            return mPlayer;
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mResumeOnFocusGain) {
                        mPlayer.play();
                        mResumeOnFocusGain = false;
                    } else if (mPlayer.isPlaying()) {
                        mPlayer.setVolume(MEDIA_VOLUME_DEFAULT);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (!mImpl.willPauseWhenDucked()) {
                        mPlayer.setVolume(MEDIA_VOLUME_DUCK);
                        break;
                    }

                    // This stream doesn't duck, so fall through and handle it the
                    // same as if it were an AUDIOFOCUS_LOSS_TRANSIENT.
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    mResumeOnFocusGain = mPlayer.isPlaying();
                    mPlayer.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    mResumeOnFocusGain = false;
                    mPlayer.stop();
                    mImpl.abandonAudioFocus();
                    break;
            }
        }
    }
}