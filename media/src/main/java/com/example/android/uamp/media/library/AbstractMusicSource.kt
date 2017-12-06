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

package com.example.android.uamp.media.library

import android.content.Context
import android.support.annotation.IntDef
import android.util.Log

@IntDef(STATE_CREATED,
        STATE_INITIALIZING,
        STATE_INITIALIZED,
        STATE_ERROR)
@Retention(AnnotationRetention.SOURCE)
annotation class State

/**
 * State indicating the source was created, but no initalization has performed.
 */
const val STATE_CREATED = 1L

/**
 * State indicating initalization of the source is in progress.
 */
const val STATE_INITIALIZING = 2L

/**
 * State indicating the source has been initialized and is ready to be used.
 */
const val STATE_INITIALIZED = 3L

/**
 * State indicating an error has occurred.
 */
const val STATE_ERROR = 4L

/**
 * Base class for music sources in UAMP.
 */
abstract class AbstractMusicSource(val context: Context) {
    @State
    var state: Long = STATE_CREATED
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    /**
     * Performs an action when this MusicSource is ready.
     *
     * This method is *not* threadsafe. Ensure actions and state changes are only performed
     * on a single thread.
     */
    fun whenReady(performAction: (Boolean) -> Unit): Boolean =
            when (state) {
                STATE_CREATED, STATE_INITIALIZING -> {
                    onReadyListeners += performAction
                    false
                }
                else -> {
                    performAction(state != STATE_ERROR)
                    true
                }
            }
}