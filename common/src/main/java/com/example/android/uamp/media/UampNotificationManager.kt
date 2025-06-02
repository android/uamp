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

package com.example.android.uamp.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.example.android.uamp.media.extensions.displayIconUri
import com.example.android.uamp.media.extensions.title
import com.example.android.uamp.media.extensions.artist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val NOW_PLAYING_CHANNEL_ID = "com.example.android.uamp.media.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION_ID = 0xb339

/**
 * A wrapper class for ExoPlayer's PlayerNotificationManager. It sets up the notification shown to
 * the user during audio playback and provides track metadata, such as track title and icon image.
 */
@UnstableApi
class UampNotificationManager(
    private val context: Context,
    sessionToken: androidx.media3.session.SessionToken,
    notificationListener: PlayerNotificationManager.NotificationListener
) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val notificationBuilder: NotificationCompat.Builder =
        NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL_ID)

    private val playerNotificationManager: PlayerNotificationManager

    var currentNotification: Notification? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        playerNotificationManager = PlayerNotificationManager.Builder(
            context,
            NOW_PLAYING_NOTIFICATION_ID,
            NOW_PLAYING_CHANNEL_ID
        )
            .setMediaDescriptionAdapter(DescriptionAdapter())
            .setNotificationListener(notificationListener)
            .build()
            .apply {
                // Note: sessionToken compatibility will be handled elsewhere if needed
                // setMediaSessionToken(sessionToken)
            }
    }

    fun hideNotification() {
        playerNotificationManager.setPlayer(null)
    }

    fun showNotificationForPlayer(player: Player) {
        playerNotificationManager.setPlayer(player)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(NOW_PLAYING_CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(
                NOW_PLAYING_CHANNEL_ID,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private inner class DescriptionAdapter : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return player.mediaMetadata.title ?: ""
        }

        override fun createCurrentContentIntent(player: Player): android.app.PendingIntent? {
            return null // We don't have a content intent in this example
        }

        override fun getCurrentContentText(player: Player): CharSequence {
            return player.mediaMetadata.artist ?: ""
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            val iconUri = player.mediaMetadata.displayIconUri
            return if (iconUri != null) {
                serviceScope.launch {
                    resolveUriAsBitmap(iconUri)?.let { callback.onBitmap(it) }
                }
                null
            } else {
                null
            }
        }

        private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
            return withContext(Dispatchers.IO) {
                try {
                    // Load the bitmap using BitmapFactory
                    val inputStream = context.contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px
