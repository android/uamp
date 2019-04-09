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

package com.example.android.uamp.aae

import android.accounts.AccountManager
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.MediaSessionCompat.Callback
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.edit
import com.example.android.uamp.media.MusicService
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** UAMP specific command for logging into the service. */
const val LOGIN = "com.example.android.uamp.aae.COMMAND.LOGIN"

/** UAMP specific command for logging out of the service. */
const val LOGOUT = "com.example.android.uamp.aae.COMMAND.LOGOUT"

const val LOGIN_EMAIL = "com.example.android.uamp.aae.ARGS.LOGIN_EMAIL"
const val LOGIN_PASSWORD = "com.example.android.uamp.aae.ARGS.LOGIN_PASSWORD"

typealias CommandHandler = (parameters: Bundle) -> Int

/**
 * Android Auto Embedded specific extensions for [MusicService].
 *
 * UAMP for Android Automotive OS requires the user to login in order to demonstrate
 * how authentication works on the system. If this doesn't apply to your application,
 * this class can be skipped in favor of its parent, [MusicService].
 *
 * If you'd like to support authentication, but not prevent using the system,
 * comment out the calls to [requireLogin].
 */
class AaeMusicService : MusicService() {

    private var isAuthenticated = false


    @ExperimentalCoroutinesApi
    override fun onCreate() {
        super.onCreate()

        mediaSession.setCallback(commandCallback)
        isAuthenticated = verifyLogin()

        if (!isAuthenticated) {
            requireLogin()
        }
    }

    private fun onLogin(email: String, password: String): Boolean {
        Log.i(TAG, "User logged in: $email")
        getSharedPreferences(AaeMusicService::class.java.name, Context.MODE_PRIVATE).edit {
            putString(USER_TOKEN, "$email:${password.hashCode()}")
        }
        return true
    }

    private fun onLogout(): Boolean {
        Log.i(TAG, "User logged out")
        getSharedPreferences(AaeMusicService::class.java.name, Context.MODE_PRIVATE).edit {
            remove(USER_TOKEN)
        }
        return false
    }

    /**
     * Verifies if the user has logged into the system.
     * In a real system, credentials should probably be handled by the
     * [AccountManager] APIs.
     */
    private fun verifyLogin() =
        getSharedPreferences(AaeMusicService::class.java.name, Context.MODE_PRIVATE)
            .contains(USER_TOKEN)

    /**
     * Sets [PlaybackStateCompat] values to indicate the user must login to continue.
     *
     * This routine sets the playback state and provides the resolution [PendingIntent]
     * that Android Automotive OS requires.
     */
    private fun requireLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java)
        val loginActivityPendingIntent = PendingIntent.getActivity(this, 0, loginIntent, 0)
        val extras = Bundle().apply {
            putString(ERROR_RESOLUTION_ACTION_LABEL, getString(R.string.error_login_button))
            putParcelable(ERROR_RESOLUTION_ACTION_INTENT, loginActivityPendingIntent)
        }

        // Sets the playback state to an error state
        // to notify subscribers that authentication is required
        val playbackState = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
            .setErrorMessage(
                PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED,
                getString(R.string.error_require_login)
            )
            .setExtras(extras)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    /**
     * Command handling routine. This should eventually be taken care of by the ExoPlayer
     * [MediaSessionConnector].
     */
    private val commandCallback = object : Callback() {
        override fun onCommand(command: String?, extras: Bundle?, resultReceiver: ResultReceiver?) {
            val parameters = extras ?: Bundle.EMPTY
            val returnCode = when (command) {
                LOGIN -> loginCommand(parameters)
                LOGOUT -> logoutCommand(parameters)
                else -> {
                    super.onCommand(command, extras, resultReceiver)
                    null
                }
            }

            // Send back a return code.
            returnCode?.let { resultReceiver?.send(returnCode, Bundle.EMPTY) }
        }
    }

    private val loginCommand: CommandHandler = { extras ->
        val email = extras.getString(LOGIN_EMAIL) ?: ""
        val password = extras.getString(LOGIN_PASSWORD) ?: ""

        isAuthenticated = onLogin(email, password)
        if (isAuthenticated) {
            // Send updated state now that the user has logged in.
            mediaSessionConnector.invalidateMediaSessionPlaybackState()

            Activity.RESULT_OK
        } else {
            // Login is required - note this.
            requireLogin()

            Activity.RESULT_CANCELED
        }
    }

    private val logoutCommand: CommandHandler = { _ ->
        isAuthenticated = onLogout()

        // Login is required - note this.
        requireLogin()
        Activity.RESULT_OK
    }
}

private const val TAG = "AaeMusicService"
private const val ERROR_RESOLUTION_ACTION_LABEL =
    "android.media.extras.ERROR_RESOLUTION_ACTION_LABEL"
private const val ERROR_RESOLUTION_ACTION_INTENT =
    "android.media.extras.ERROR_RESOLUTION_ACTION_INTENT"

private const val USER_TOKEN = "com.example.android.uamp.aae.PREFS.USER_TOKEN"