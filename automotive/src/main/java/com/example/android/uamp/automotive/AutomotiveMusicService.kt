/*
 * Copyright 2019 Google Inc. All rights reserved.
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

package com.example.android.uamp.automotive

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.android.uamp.media.UampNotificationManager
import com.example.android.uamp.media.library.JsonSource
import com.example.android.uamp.media.library.MusicSource
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** UAMP specific command for logging into the service. */
const val LOGIN = "com.example.android.uamp.automotive.COMMAND.LOGIN"

/** UAMP specific command for logging out of the service. */
const val LOGOUT = "com.example.android.uamp.automotive.COMMAND.LOGOUT"

const val LOGIN_EMAIL = "com.example.android.uamp.automotive.ARGS.LOGIN_EMAIL"
const val LOGIN_PASSWORD = "com.example.android.uamp.automotive.ARGS.LOGIN_PASSWORD"

/**
 * Android Automotive specific MediaSessionService.
 *
 * This service provides music playback functionality specifically optimized for
 * Android Automotive OS, including authentication flows and automotive-specific features.
 */
class AutomotiveMusicService : MediaSessionService() {

    private lateinit var notificationManager: UampNotificationManager
    private lateinit var mediaSource: MusicSource
    private lateinit var player: Player
    private lateinit var mediaSession: MediaSession
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private val remoteJsonSource: Uri =
        Uri.parse("https://storage.googleapis.com/uamp/catalog.json")

    private val uAmpAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    /**
     * Configure ExoPlayer to handle audio focus for us.
     */
    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(uAmpAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize player
        player = exoPlayer

        // Check authentication
        if (!isAuthenticated()) {
            Log.d(TAG, "User not authenticated, requiring login for automotive")
        }

        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(
                    this, 
                    0, 
                    sessionIntent, 
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        // Create a new MediaSession with custom callback for automotive commands
        mediaSession = MediaSession.Builder(this, player)
            .apply {
                sessionActivityPendingIntent?.let { setSessionActivity(it) }
            }
            .setCallback(AutomotiveMediaSessionCallback())
            .build()

        // Initialize notification manager
        notificationManager = UampNotificationManager(
            this,
            mediaSession.token,
            object : androidx.media3.ui.PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopSelf()
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: android.app.Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    }
                }
            }
        )

        // Initialize media source
        mediaSource = JsonSource(source = remoteJsonSource)
        serviceScope.launch {
            Log.d(TAG, "Starting to load catalog from: $remoteJsonSource")
            mediaSource.load()
            
            mediaSource.whenReady { success ->
                if (success) {
                    val catalogSize = mediaSource.count()
                    Log.d(TAG, "Catalog loading completed successfully. Items: $catalogSize")
                } else {
                    Log.e(TAG, "Catalog loading failed")
                }
            }
        }

        // Show notification for the player
        notificationManager.showNotificationForPlayer(exoPlayer)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        serviceJob.cancel()
        exoPlayer.release()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun onLogin(email: String, password: String): Boolean {
        Log.i(TAG, "User logged in: $email")
        getSharedPreferences(AutomotiveMusicService::class.java.name, Context.MODE_PRIVATE).edit {
            putString(USER_TOKEN, "$email:${password.hashCode()}")
        }
        return true
    }

    private fun onLogout(): Boolean {
        Log.i(TAG, "User logged out")
        getSharedPreferences(AutomotiveMusicService::class.java.name, Context.MODE_PRIVATE).edit {
            remove(USER_TOKEN)
        }
        return true
    }

    /**
     * Verifies if the user has logged into the system.
     */
    private fun isAuthenticated() =
        getSharedPreferences(AutomotiveMusicService::class.java.name, Context.MODE_PRIVATE)
            .contains(USER_TOKEN)

    /**
     * Custom MediaSession callback to handle automotive-specific commands
     */
    private inner class AutomotiveMediaSessionCallback : MediaSession.Callback {
        
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // For automotive, we can be more permissive with connections
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return when (customCommand.customAction) {
                LOGIN -> {
                    val email = args.getString(LOGIN_EMAIL) ?: ""
                    val password = args.getString(LOGIN_PASSWORD) ?: ""
                    
                    val success = onLogin(email, password)
                    
                    Futures.immediateFuture(
                        if (success) {
                            SessionResult(SessionResult.RESULT_SUCCESS)
                        } else {
                            SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                        }
                    )
                }
                LOGOUT -> {
                    onLogout()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> {
                    super.onCustomCommand(session, controller, customCommand, args)
                }
            }
        }

        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // For automotive, allow media playback
            return super.onSetMediaItems(session, controller, mediaItems, startIndex, startPositionMs)
        }
    }
}

private const val TAG = "AutomotiveMusicService"
private const val USER_TOKEN = "com.example.android.uamp.automotive.PREFS.USER_TOKEN"