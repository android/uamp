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

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.media.AudioAttributesCompat;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Compatibility version of an {@link AudioFocusRequest}.
 */
public class AudioFocusRequestCompat {

    @Retention(SOURCE)
    @IntDef({
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
    })
    public @interface FocusGain {}

    private final int mFocusGain;
    private final OnAudioFocusChangeListener mOnAudioFocusChangeListener;
    private final Handler mFocusChangeHandler;
    private final AudioAttributesCompat mAudioAttributesCompat;
    private final boolean mPauseOnDuck;
    private final boolean mAcceptsDelayedFocusGain;

    private AudioFocusRequestCompat(int focusGain,
                                    OnAudioFocusChangeListener onAudioFocusChangeListener,
                                    Handler focusChangeHandler,
                                    AudioAttributesCompat audioAttributesCompat,
                                    boolean pauseOnDuck,
                                    boolean acceptsDelayedFocusGain) {
        mFocusGain = focusGain;
        mOnAudioFocusChangeListener = onAudioFocusChangeListener;
        mFocusChangeHandler = focusChangeHandler;
        mAudioAttributesCompat = audioAttributesCompat;
        mPauseOnDuck = pauseOnDuck;
        mAcceptsDelayedFocusGain = acceptsDelayedFocusGain;
    }

    public int getFocusGain() {
        return mFocusGain;
    }

    public AudioAttributesCompat getAudioAttributesCompat() {
        return mAudioAttributesCompat;
    }

    public boolean willPauseWhenDucked() {
        return mPauseOnDuck;
    }

    public boolean acceptsDelayedFocusGain() {
        return mAcceptsDelayedFocusGain;
    }

    /* package */ OnAudioFocusChangeListener getOnAudioFocusChangeListener() {
        return mOnAudioFocusChangeListener;
    }

    /* package */ Handler getFocusChangeHandler() {
        return mFocusChangeHandler;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    /* package */ AudioAttributes getAudioAttributes() {
        return (mAudioAttributesCompat != null)
                ? (AudioAttributes) (mAudioAttributesCompat.unwrap())
                : null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    /* package */ AudioFocusRequest getAudioFocusRequest() {
        return new AudioFocusRequest.Builder(mFocusGain)
                .setAudioAttributes(getAudioAttributes())
                .setAcceptsDelayedFocusGain(mAcceptsDelayedFocusGain)
                .setWillPauseWhenDucked(mPauseOnDuck)
                .setOnAudioFocusChangeListener(mOnAudioFocusChangeListener, mFocusChangeHandler)
                .build();
    }

    /**
     * Builder for an {@link AudioFocusRequestCompat}.
     */
    public static final class Builder {
        private int mFocusGain;
        private OnAudioFocusChangeListener mOnAudioFocusChangeListener;
        private Handler mFocusChangeHandler;
        private AudioAttributesCompat mAudioAttributesCompat;

        // Flags
        private boolean mPauseOnDuck;
        private boolean mAcceptsDelayedFocusGain;

        public Builder(@FocusGain int focusGain) {
            mFocusGain = focusGain;
        }

        public Builder(@NonNull AudioFocusRequestCompat requestToCopy) {
            mFocusGain = requestToCopy.mFocusGain;
            mOnAudioFocusChangeListener = requestToCopy.mOnAudioFocusChangeListener;
            mFocusChangeHandler = requestToCopy.mFocusChangeHandler;
            mAudioAttributesCompat = requestToCopy.mAudioAttributesCompat;
            mPauseOnDuck = requestToCopy.mPauseOnDuck;
            mAcceptsDelayedFocusGain = requestToCopy.mAcceptsDelayedFocusGain;
        }

        @NonNull
        public Builder setFocusGain(@FocusGain int focusGain) {
            mFocusGain = focusGain;
            return this;
        }

        @NonNull
        public Builder setOnAudioFocusChangeListener(@NonNull OnAudioFocusChangeListener listener) {
            return setOnAudioFocusChangeListener(listener, new Handler(Looper.getMainLooper()));
        }

        @NonNull
        public Builder setOnAudioFocusChangeListener(@NonNull OnAudioFocusChangeListener listener,
                                                     @NonNull Handler handler) {
            mOnAudioFocusChangeListener = listener;
            mFocusChangeHandler = handler;
            return this;
        }

        @NonNull
        public Builder setAudioAttributes(@NonNull AudioAttributesCompat attributes) {
            mAudioAttributesCompat = attributes;
            return this;
        }

        @NonNull
        public Builder setWillPauseWhenDucked(boolean pauseOnDuck) {
            mPauseOnDuck = pauseOnDuck;
            return this;
        }

        @NonNull
        public Builder setAcceptsDelayedFocusGain(boolean acceptsDelayedFocusGain) {
            mAcceptsDelayedFocusGain = acceptsDelayedFocusGain;
            return this;
        }

        public AudioFocusRequestCompat build() {
            return new AudioFocusRequestCompat(mFocusGain,
                    mOnAudioFocusChangeListener,
                    mFocusChangeHandler,
                    mAudioAttributesCompat,
                    mPauseOnDuck,
                    mAcceptsDelayedFocusGain);
        }
    }
}
