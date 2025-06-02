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

import android.accounts.AccountManager
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.edit
import com.example.android.uamp.media.MusicService
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.example.android.uamp.media.library.BrowseTree
import com.example.android.uamp.media.library.JsonSource
import com.example.android.uamp.media.library.MEDIA_SEARCH_SUPPORTED
import com.example.android.uamp.media.library.MusicSource
import com.example.android.uamp.media.library.UAMP_BROWSABLE_ROOT
import com.example.android.uamp.media.library.UAMP_EMPTY_ROOT

/** UAMP specific command for logging into the service. */
const val LOGIN = "com.example.android.uamp.automotive.COMMAND.LOGIN"

/** UAMP specific command for logging out of the service. */
const val LOGOUT = "com.example.android.uamp.automotive.COMMAND.LOGOUT"

const val LOGIN_EMAIL = "com.example.android.uamp.automotive.ARGS.LOGIN_EMAIL"
const val LOGIN_PASSWORD = "com.example.android.uamp.automotive.ARGS.LOGIN_PASSWORD"

typealias CommandHandler = (parameters: Bundle, callback: ResultReceiver?) -> Boolean

/**
 * Android Automotive specific extensions for [MusicService].
 *
 * UAMP for Android Automotive OS requires the user to login in order to demonstrate
 * how authentication works on the system. If this doesn't apply to your application,
 * this class can be skipped in favor of its parent, [MusicService].
 *
 * If you'd like to support authentication, but not prevent using the system,
 * comment out the calls to [requireLogin].
 */
class AutomotiveMusicService : MusicService() {

    override fun onCreate() {
        super.onCreate()

        // Register to handle login/logout commands.
        mediaSessionConnector.registerCustomCommandReceiver(AutomotiveCommandReceiver())

        // Require the user to be logged in for demonstration purposes.
        if (!isAuthenticated()) {
            requireLogin()
        }

        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        // Create a new MediaSession.
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        /*
         * By default, all known clients are permitted to search, but only tell unknown callers
         * about search if permitted by the [BrowseTree].
         */
        val isKnownCaller = packageValidator.isKnownCaller(clientPackageName, clientUid)
        val rootExtras = Bundle().apply {
            putBoolean(
                MEDIA_SEARCH_SUPPORTED,
                isKnownCaller || browseTree.searchableByUnknownCaller
            )
        }

        return if (isKnownCaller) {
            BrowserRoot(UAMP_BROWSABLE_ROOT, rootExtras)
        } else {
            BrowserRoot(UAMP_EMPTY_ROOT, rootExtras)
        }
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
        return false
    }

    /**
     * Verifies if the user has logged into the system.
     * In a real system, credentials should probably be handled by the
     * [AccountManager] APIs.
     */
    private fun isAuthenticated() =
        getSharedPreferences(AutomotiveMusicService::class.java.name, Context.MODE_PRIVATE)
            .contains(USER_TOKEN)

    /**
     * Sets [PlaybackStateCompat] values to indicate the user must login to continue.
     *
     * This routine sets the playback state and provides the resolution [PendingIntent]
     * that Android Automotive OS requires.
     */
    private fun requireLogin() {
        val loginIntent = Intent(this, SignInActivity::class.java)
        val loginActivityPendingIntent = PendingIntent.getActivity(this, 0, loginIntent, 0)
        val extras = Bundle().apply {
            putString(ERROR_RESOLUTION_ACTION_LABEL, getString(R.string.error_login_button))
            putParcelable(ERROR_RESOLUTION_ACTION_INTENT, loginActivityPendingIntent)
        }
        mediaSessionConnector.setCustomErrorMessage(
            getString(R.string.error_require_login),
            PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED,
            extras
        )
    }

    /**
     * This is the entry point for custom commands received by ExoPlayer's
     * [MediaSessionConnector.customCommandReceivers].
     *
     * The extension will call each [CommandReceiver] in turn. If the [CommandReceiver] can
     * handle the command, it returns `true` to indicate the command's been handled and
     * processing should stop. If the [CommandReceiver] cannot/doesn't want to handle the
     * command, it should return `false`.
     *
     * We simplify this a bit by having our own [CommandHandler] that works with a single
     * command (either "log in" or "log out"). Each of these returns true at the end of its
     * processing.
     *
     * If the command received isn't either of our commands, we just return `false`.
     *
     * Suppress the warning because the original name, `cb` is not as clear as to its purpose.
     */
    private inner class AutomotiveCommandReceiver : MediaSessionConnector.CommandReceiver {
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun onCommand(
            player: Player,
            command: String,
            extras: Bundle?,
            callback: ResultReceiver?
        ): Boolean =
            when (command) {
                LOGIN -> loginCommand(extras ?: Bundle.EMPTY, callback)
                LOGOUT -> logoutCommand(extras ?: Bundle.EMPTY, callback)
                else -> false
            }
    }

    private val loginCommand: CommandHandler = { extras, callback ->
        val email = extras.getString(LOGIN_EMAIL) ?: ""
        val password = extras.getString(LOGIN_PASSWORD) ?: ""

        if (onLogin(email, password)) {
            // Updated state (including clearing the error) now that the user has logged in.
            mediaSessionConnector.setCustomErrorMessage(null)
            mediaSessionConnector.invalidateMediaSessionPlaybackState()

            callback?.send(Activity.RESULT_OK, Bundle.EMPTY)
        } else {
            // Login is required - note this.
            requireLogin()

            callback?.send(Activity.RESULT_CANCELED, Bundle.EMPTY)
        }
        true
    }

    private val logoutCommand: CommandHandler = { _, callback ->
        // Log the user out.
        onLogout()

        // Login is required - note this.
        requireLogin()
        callback?.send(Activity.RESULT_OK, Bundle.EMPTY)
        true
    }
}

private const val TAG = "AutomotiveMusicService"
private const val ERROR_RESOLUTION_ACTION_LABEL =
    "android.media.extras.ERROR_RESOLUTION_ACTION_LABEL"
private const val ERROR_RESOLUTION_ACTION_INTENT =
    "android.media.extras.ERROR_RESOLUTION_ACTION_INTENT"

private const val USER_TOKEN = "com.example.android.uamp.automotive.PREFS.USER_TOKEN"