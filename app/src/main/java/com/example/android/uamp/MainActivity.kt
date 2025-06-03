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

import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.android.uamp.databinding.ActivityMainBinding
import com.example.android.uamp.fragments.MediaItemFragment
import com.example.android.uamp.utils.InjectorUtils
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.google.android.gms.cast.framework.CastContext

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainActivityViewModel> {
        InjectorUtils.provideMainActivityViewModel(this)
    }
    private var castContext: CastContext? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Cast context for media casting functionality
        castContext = CastContext.getSharedInstance(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Since Mixtape is a music player, the volume controls should adjust the music volume while
        // in the app.
        volumeControlStream = AudioManager.STREAM_MUSIC

        /**
         * Observe changes to the [MainActivityViewModel.navigateToFragment] property and either
         * navigate to the fragment or show the cast dialog.
         */
        viewModel.navigateToMediaItem.observe(this) { event ->
            event?.getContentIfNotHandled()?.let { mediaId ->
                navigateToMediaItem(mediaId)
            }
        }

        /**
         * Observe changes to the connection state and root media ID.
         * Create the main fragment when both are ready.
         */
        viewModel.isConnected.observe(this) { connected: Boolean ->
            Log.d(TAG, "Connection state changed: $connected")
            if (connected) {
                checkAndCreateMainFragment()
            }
        }
        
        viewModel.musicServiceConnection.rootMediaId.observe(this) { rootMediaId ->
            Log.d(TAG, "Root media ID changed: $rootMediaId")
            if (!rootMediaId.isNullOrEmpty()) {
                checkAndCreateMainFragment()
            }
        }
    }

    private fun navigateToMediaItem(mediaId: String) {
        var fragment: MediaItemFragment? = getBrowseFragment(mediaId)
        if (fragment == null) {
            fragment = MediaItemFragment.newInstance(mediaId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.mediaItemFragment, fragment, mediaId)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun getBrowseFragment(mediaId: String): MediaItemFragment? {
        return supportFragmentManager.findFragmentByTag(mediaId) as MediaItemFragment?
    }

    private fun checkAndCreateMainFragment() {
        val isConnected = viewModel.isConnected.value == true
        val rootMediaId = viewModel.musicServiceConnection.rootMediaId.value
        
        Log.d(TAG, "checkAndCreateMainFragment: isConnected=$isConnected, rootMediaId=$rootMediaId")
        
        if (isConnected && !rootMediaId.isNullOrEmpty()) {
            // Only create fragment if it doesn't already exist
            val existingFragment = supportFragmentManager.findFragmentById(R.id.mediaItemFragment)
            if (existingFragment == null) {
                Log.d(TAG, "Creating MediaItemFragment with rootMediaId: $rootMediaId")
                val fragment = MediaItemFragment.newInstance(rootMediaId)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.mediaItemFragment, fragment)
                    .commit()
            } else {
                Log.d(TAG, "MediaItemFragment already exists")
            }
        } else {
            Log.d(TAG, "Not ready to create fragment yet")
        }
    }
}

private const val TAG = "MainActivity"
