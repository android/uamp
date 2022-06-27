package com.example.android.uamp.automotive

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT_COMPAT
import androidx.media3.session.MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL_COMPAT
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.android.uamp.media.MusicService
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** UAMP specific command for logging into the service. */
const val LOGIN = "com.example.android.uamp.automotive.COMMAND.LOGIN"

/** UAMP specific command for logging out of the service. */
const val LOGOUT = "com.example.android.uamp.automotive.COMMAND.LOGOUT"

const val LOGIN_EMAIL = "com.example.android.uamp.automotive.ARGS.LOGIN_EMAIL"
const val LOGIN_PASSWORD = "com.example.android.uamp.automotive.ARGS.LOGIN_PASSWORD"

class AutomotiveMusicService: MusicService() {

    override fun getCallback(): MediaLibrarySession.Callback {
        return AutomotiveCallback()
    }

    private inner class AutomotiveCallback() : MusicServiceCallback() {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands =
                connectionResult.availableSessionCommands
                    .buildUpon()
                    .add(SessionCommand(LOGIN, Bundle()))
                    .add(SessionCommand(LOGOUT, Bundle()))
                    .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands, connectionResult.availablePlayerCommands)
        }

        @OptIn(UnstableApi::class)
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return if (isAuthenticated()) {
                super.onGetChildren(session, browser, parentId, page, pageSize, params)
            } else {
                Futures.immediateFuture(
                    LibraryResult.ofError(
                        LibraryResult.RESULT_ERROR_SESSION_AUTHENTICATION_EXPIRED,
                        LibraryParams.Builder()
                            .setExtras(getExpiredAuthenticationResolutionExtras()).build()
                    )
                )
            }
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                LOGIN -> {
                    onLogin(args.getString(LOGIN_EMAIL) ?: "", args.getString(LOGIN_PASSWORD) ?: "")
                }
                LOGOUT -> onLogout()
                else -> {
                    return Futures.immediateFuture(
                        SessionResult(
                            SessionResult.RESULT_ERROR_SESSION_AUTHENTICATION_EXPIRED,
                            getExpiredAuthenticationResolutionExtras()
                        )
                    )
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    /**
     * Fakes the verification of email and password and authenticates the user. Use the
     * authentication technique of your choice in your app.
     *
     * <p>Returns true if the user is supposed to be successfully authenticated.
     */
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
     * Whether the user has been authenticated.
     */
    private fun isAuthenticated() =
        getSharedPreferences(AutomotiveMusicService::class.java.name, Context.MODE_PRIVATE)
            .contains(USER_TOKEN)

    private fun getExpiredAuthenticationResolutionExtras(): Bundle {
        return Bundle().also {
            it.putString(
                EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL_COMPAT,
                getString(R.string.login_button_label))
            val signInIntent = Intent(this, SignInActivity::class.java)
            @OptIn(UnstableApi::class)
            val flags = if (Util.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            it.putParcelable(
                EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT_COMPAT,
                PendingIntent.getActivity(this, /* requestCode= */ 0, signInIntent, flags))
        }
    }
}

private const val TAG = "AutomotiveMusicService"
private const val USER_TOKEN = "com.example.android.uamp.automotive.PREFS.USER_TOKEN"
