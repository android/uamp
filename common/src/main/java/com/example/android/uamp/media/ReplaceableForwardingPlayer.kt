/*
 * Copyright 2022 Google Inc. All rights reserved.
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

import android.annotation.SuppressLint
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION
import androidx.media3.common.Player.EVENT_MEDIA_METADATA_CHANGED
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import java.util.ArrayDeque
import kotlin.math.min

/**
 * A [Player] implementation that delegates to an actual [Player] implementation that is
 * replaceable by another instance by calling [setPlayer].
 */
@SuppressLint("UnsafeOptInUsageError")
class ReplaceableForwardingPlayer(private var player: Player) : Player {

    private val listeners: MutableList<Player.Listener> = arrayListOf()
    // After disconnecting from the Cast device, the timeline of the CastPlayer is empty, so we
    // need to track the playlist to be able to transfer the playlist back to the local player after
    // having disconnected.
    private val playlist: MutableList<MediaItem> = arrayListOf()
    private var currentMediaItemIndex: Int = 0

    private val playerListener: Player.Listener = PlayerListener()

    init {
        player.addListener(playerListener)
    }

    /** Sets a new [Player] instance to which the state of the previous player is transferred. */
    fun setPlayer(player: Player) {
        // Remove add all listeners before changing the player state.
        for (listener in listeners) {
            this.player.removeListener(listener)
            player.addListener(listener)
        }
        // Add/remove our listener we use to workaround the missing metadata support of CastPlayer.
        this.player.removeListener(playerListener)
        player.addListener(playerListener)

        player.repeatMode = this.player.repeatMode
        player.shuffleModeEnabled = this.player.shuffleModeEnabled
        player.playlistMetadata = this.player.playlistMetadata
        player.trackSelectionParameters = this.player.trackSelectionParameters
        player.volume = this.player.volume
        player.playWhenReady = this.player.playWhenReady

        // Prepare the new player.
        player.setMediaItems(playlist, currentMediaItemIndex, this.player.contentPosition)
        player.prepare()

        // Stop the previous player. Don't release so it can be used again.
        this.player.clearMediaItems()
        this.player.stop()

        this.player = player
    }

    override fun getApplicationLooper(): Looper {
        return player.applicationLooper
    }

    override fun addListener(listener: Player.Listener) {
        player.addListener(listener)
        listeners.add(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        player.removeListener(listener)
        listeners.remove(listener)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        player.setMediaItems(mediaItems)
        playlist.clear()
        playlist.addAll(mediaItems)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        player.setMediaItems(mediaItems, resetPosition)
        playlist.clear()
        playlist.addAll(mediaItems)
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startWindowIndex: Int,
        startPositionMs: Long
    ) {
        player.setMediaItems(mediaItems, startWindowIndex, startPositionMs)
        playlist.clear()
        playlist.addAll(mediaItems)
    }

    override fun setMediaItem(mediaItem: MediaItem) {
        player.setMediaItem(mediaItem)
        playlist.clear()
        playlist.add(mediaItem)
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        player.setMediaItem(mediaItem, startPositionMs)
        playlist.clear()
        playlist.add(mediaItem)
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        player.setMediaItem(mediaItem, resetPosition)
        playlist.clear()
        playlist.add(mediaItem)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        player.addMediaItem(mediaItem)
        playlist.add(mediaItem)
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        player.addMediaItem(index, mediaItem)
        playlist.add(index, mediaItem)
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        player.addMediaItems(mediaItems)
        playlist.addAll(mediaItems)
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        player.addMediaItems(index, mediaItems)
        playlist.addAll(index, mediaItems)
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        player.moveMediaItem(currentIndex, newIndex)
        playlist.add(min(newIndex, playlist.size), playlist.removeAt(currentIndex))
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        val removedItems: ArrayDeque<MediaItem> = ArrayDeque()
        val removedItemsLength = toIndex - fromIndex
        for (i in removedItemsLength - 1 downTo 0) {
            removedItems.addFirst(playlist.removeAt(fromIndex + i))
        }
        playlist.addAll(min(newIndex, playlist.size), removedItems)
    }

    override fun removeMediaItem(index: Int) {
        player.removeMediaItem(index)
        playlist.removeAt(index)
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        player.removeMediaItems(fromIndex, toIndex)
        val removedItemsLength = toIndex - fromIndex
        for (i in removedItemsLength - 1 downTo 0) {
            playlist.removeAt(fromIndex + i)
        }
    }

    override fun clearMediaItems() {
        player.clearMediaItems()
        playlist.clear()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return player.isCommandAvailable(command)
    }

    override fun canAdvertiseSession(): Boolean {
        return player.canAdvertiseSession()
    }

    override fun getAvailableCommands(): Player.Commands {
        return player.availableCommands
    }

    override fun prepare() {
        player.prepare()
    }

    override fun getPlaybackState(): Int {
        return player.playbackState
    }

    override fun getPlaybackSuppressionReason(): Int {
        return player.playbackSuppressionReason
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying
    }

    override fun getPlayerError(): PlaybackException? {
        return player.playerError
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        player.playWhenReady = playWhenReady
    }

    override fun getPlayWhenReady(): Boolean {
        return player.playWhenReady
    }

    override fun setRepeatMode(repeatMode: Int) {
        player.repeatMode = repeatMode
    }

    override fun getRepeatMode(): Int {
        return player.repeatMode
    }

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        player.shuffleModeEnabled = shuffleModeEnabled
    }

    override fun getShuffleModeEnabled(): Boolean {
        return player.shuffleModeEnabled
    }

    override fun isLoading(): Boolean {
        return player.isLoading
    }

    override fun seekToDefaultPosition() {
        player.seekToDefaultPosition()
    }

    override fun seekToDefaultPosition(windowIndex: Int) {
        player.seekToDefaultPosition(windowIndex)
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    override fun seekTo(windowIndex: Int, positionMs: Long) {
        player.seekTo(windowIndex, positionMs)
    }

    override fun getSeekBackIncrement(): Long {
        return player.seekBackIncrement
    }

    override fun seekBack() {
        player.seekBack()
    }

    override fun getSeekForwardIncrement(): Long {
        return player.seekForwardIncrement
    }

    override fun seekForward() {
        player.seekForward()
    }

    override fun hasPrevious(): Boolean {
        return player.hasPrevious()
    }

    override fun hasPreviousWindow(): Boolean {
        return player.hasPreviousWindow()
    }

    override fun hasPreviousMediaItem(): Boolean {
        return player.hasPreviousMediaItem()
    }

    override fun previous() {
        player.previous()
    }

    override fun seekToPreviousWindow() {
        player.seekToPreviousWindow()
    }

    override fun seekToPreviousMediaItem() {
        player.seekToPreviousMediaItem()
    }

    override fun getMaxSeekToPreviousPosition(): Long {
        return player.maxSeekToPreviousPosition
    }

    override fun seekToPrevious() {
        player.seekToPrevious()
    }

    override fun hasNext(): Boolean {
        return player.hasNext()
    }

    override fun hasNextWindow(): Boolean {
        return player.hasNextWindow()
    }

    override fun hasNextMediaItem(): Boolean {
        return player.hasNextMediaItem()
    }

    override fun next() {
        player.next()
    }

    override fun seekToNextWindow() {
        player.seekToNextWindow()
    }

    override fun seekToNextMediaItem() {
        player.seekToNextMediaItem()
    }

    override fun seekToNext() {
        player.seekToNext()
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        player.playbackParameters = playbackParameters
    }

    override fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    override fun getPlaybackParameters(): PlaybackParameters {
        return player.playbackParameters
    }

    override fun stop() {
        player.stop()
    }

    override fun stop(reset: Boolean) {
        player.stop(reset)
        if (reset) {
            playlist.clear()
        }
    }

    override fun release() {
        player.release()
        playlist.clear()
    }

    override fun getCurrentTracks(): Tracks {
        return player.currentTracks
    }

    override fun getTrackSelectionParameters(): TrackSelectionParameters {
        return player.trackSelectionParameters
    }

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        player.trackSelectionParameters = parameters
    }

    override fun getMediaMetadata(): MediaMetadata {
        return player.mediaMetadata
    }

    override fun getPlaylistMetadata(): MediaMetadata {
        return player.playlistMetadata
    }

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        player.playlistMetadata = mediaMetadata
    }

    override fun getCurrentManifest(): Any? {
        return player.currentManifest
    }

    override fun getCurrentTimeline(): Timeline {
        return player.currentTimeline
    }

    override fun getCurrentPeriodIndex(): Int {
        return player.currentPeriodIndex
    }

    override fun getCurrentWindowIndex(): Int {
        return player.currentWindowIndex
    }

    override fun getCurrentMediaItemIndex(): Int {
        return player.currentMediaItemIndex
    }

    override fun getNextWindowIndex(): Int {
        return player.nextWindowIndex
    }

    override fun getNextMediaItemIndex(): Int {
        return player.nextMediaItemIndex
    }

    override fun getPreviousWindowIndex(): Int {
        return player.previousWindowIndex
    }

    override fun getPreviousMediaItemIndex(): Int {
        return player.previousMediaItemIndex
    }

    override fun getCurrentMediaItem(): MediaItem? {
        return player.currentMediaItem
    }

    override fun getMediaItemCount(): Int {
        return player.mediaItemCount
    }

    override fun getMediaItemAt(index: Int): MediaItem {
        return player.getMediaItemAt(index)
    }

    override fun getDuration(): Long {
        return player.duration
    }

    override fun getCurrentPosition(): Long {
        return player.currentPosition
    }

    override fun getBufferedPosition(): Long {
        return player.bufferedPosition
    }

    override fun getBufferedPercentage(): Int {
        return player.bufferedPercentage
    }

    override fun getTotalBufferedDuration(): Long {
        return player.totalBufferedDuration
    }

    override fun isCurrentWindowDynamic(): Boolean {
        return player.isCurrentWindowDynamic
    }

    override fun isCurrentMediaItemDynamic(): Boolean {
        return player.isCurrentMediaItemDynamic
    }

    override fun isCurrentWindowLive(): Boolean {
        return player.isCurrentWindowLive
    }

    override fun isCurrentMediaItemLive(): Boolean {
        return player.isCurrentMediaItemLive
    }

    override fun getCurrentLiveOffset(): Long {
        return player.currentLiveOffset
    }

    override fun isCurrentWindowSeekable(): Boolean {
        return player.isCurrentWindowSeekable
    }

    override fun isCurrentMediaItemSeekable(): Boolean {
        return player.isCurrentMediaItemSeekable
    }

    override fun isPlayingAd(): Boolean {
        return player.isPlayingAd
    }

    override fun getCurrentAdGroupIndex(): Int {
        return player.currentAdGroupIndex
    }

    override fun getCurrentAdIndexInAdGroup(): Int {
        return player.currentAdIndexInAdGroup
    }

    override fun getContentDuration(): Long {
        return player.contentDuration
    }

    override fun getContentPosition(): Long {
        return player.contentPosition
    }

    override fun getContentBufferedPosition(): Long {
        return player.contentBufferedPosition
    }

    override fun getAudioAttributes(): AudioAttributes {
        return player.audioAttributes
    }

    override fun setVolume(volume: Float) {
        player.volume = volume
    }

    override fun getVolume(): Float {
        return player.volume
    }

    override fun clearVideoSurface() {
        player.clearVideoSurface()
    }

    override fun clearVideoSurface(surface: Surface?) {
        player.clearVideoSurface(surface)
    }

    override fun setVideoSurface(surface: Surface?) {
        player.setVideoSurface(surface)
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        player.setVideoSurfaceHolder(surfaceHolder)
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        player.clearVideoSurfaceHolder(surfaceHolder)
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        return player.setVideoSurfaceView(surfaceView)
    }

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        return player.clearVideoSurfaceView(surfaceView)
    }

    override fun setVideoTextureView(textureView: TextureView?) {
        return player.setVideoTextureView(textureView)
    }

    override fun clearVideoTextureView(textureView: TextureView?) {
        return player.clearVideoTextureView(textureView)
    }

    override fun getVideoSize(): VideoSize {
        return player.videoSize
    }

    override fun getCurrentCues(): CueGroup {
        return player.currentCues
    }

    override fun getDeviceInfo(): DeviceInfo {
        return player.deviceInfo
    }

    override fun getDeviceVolume(): Int {
        return player.deviceVolume
    }

    override fun isDeviceMuted(): Boolean {
        return player.isDeviceMuted
    }

    override fun setDeviceVolume(volume: Int) {
        player.deviceVolume = volume
    }

    override fun increaseDeviceVolume() {
        player.increaseDeviceVolume()
    }

    override fun decreaseDeviceVolume() {
        player.decreaseDeviceVolume()
    }

    override fun setDeviceMuted(muted: Boolean) {
        player.isDeviceMuted = muted
    }

    private inner class PlayerListener : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(EVENT_MEDIA_ITEM_TRANSITION)
                && !events.contains(EVENT_MEDIA_METADATA_CHANGED)) {
                // CastPlayer does not support onMetaDataChange. We can trigger this here when the
                // media item changes.
                if (playlist.isNotEmpty()) {
                    for (listener in listeners) {
                        listener.onMediaMetadataChanged(
                            playlist[player.currentMediaItemIndex].mediaMetadata
                        )
                    }
                }
            }
            if (events.contains(EVENT_POSITION_DISCONTINUITY)
                || events.contains(EVENT_MEDIA_ITEM_TRANSITION)
                || events.contains(EVENT_TIMELINE_CHANGED)) {
                if (!player.currentTimeline.isEmpty) {
                    currentMediaItemIndex = player.currentMediaItemIndex
                }
            }
        }
    }
}