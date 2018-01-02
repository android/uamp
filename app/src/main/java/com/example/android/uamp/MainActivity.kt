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

package com.example.android.uamp

import android.arch.lifecycle.ViewModelProviders
import android.media.AudioManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity(), MediaBrowserStateChangeCallback {

    private lateinit var mediaBrowserConnection: MediaBrowserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Since UAMP is a music player, the volume controls should adjust the music
        // volume while in the app.
        setVolumeControlStream(AudioManager.STREAM_MUSIC)

        mediaBrowserConnection = ViewModelProviders.of(this).get(MediaBrowserViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        mediaBrowserConnection.registerCallback(this)
    }

    override fun onStop() {
        super.onStop()
        mediaBrowserConnection.unregisterCallback(this)
    }

    override fun onConnected() {
        super.onConnected()

        navigateToBrowser(mediaBrowserConnection.getRoot())
    }

    private fun navigateToBrowser(mediaId: String) {
        var fragment: MediaItemFragment? = getBrowseFragment(mediaId)

        if (fragment == null) {
            fragment = MediaItemFragment.newInstance(mediaId)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.browse_fragment, fragment, mediaId)

            // If this is not the top level media (root), we add it to the fragment back stack,
            // so that actionbar toggle and Back will work appropriately:
            if (mediaId != mediaBrowserConnection.getRoot()) {
                transaction.addToBackStack(null)
            }
            transaction.commit()
        }
    }

    private fun getBrowseFragment(mediaId: String): MediaItemFragment? {
        return fragmentManager.findFragmentByTag(mediaId) as MediaItemFragment?
    }
}
