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
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.android.uamp.fragments.MEDIA_ID_ARG
import com.example.android.uamp.fragments.MediaItemFragment
import com.example.android.uamp.media.MusicService
import com.example.android.uamp.utils.Event
import com.example.android.uamp.viewmodels.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        setContentView(R.layout.activity_main)

        // Since UAMP is a music player, the volume controls should adjust the music volume while
        // in the app.
        volumeControlStream = AudioManager.STREAM_MUSIC

        /**
         * Observe [MainActivityViewModel.navigateToFragment] for [Event]s that request a
         * fragment swap.
         */
        viewModel.navigateToFragment.observe(this) {
            it?.getContentIfNotHandled()?.let { fragmentRequest ->
                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(
                    R.id.fragmentContainer, fragmentRequest.fragment, fragmentRequest.tag
                )
                if (fragmentRequest.backStack) transaction.addToBackStack(null)
                transaction.commit()
            }
        }

        /**
         * Observe changes to the [MainActivityViewModel.rootMediaItem]. When the app starts,
         * and the UI connects to [MusicService], this will be updated and the app will show
         * the initial list of media items.
         */
        viewModel.rootMediaItem.observe(this) { rootMediaItem ->
            Log.d(TAG, "rootMediaItem: ${rootMediaItem?.mediaId}")
            rootMediaItem?.let { navigateToMediaItem(it.mediaId) }
        }

        /**
         * Observe [MainActivityViewModel.navigateToMediaItem] for [Event]s indicating
         * the user has requested to browse to a different [MediaItemData].
         */
        viewModel.navigateToMediaItem.observe(this) {
            Log.d(TAG, "navigateToMediaItem: ${it.peekContent()}")
            it?.getContentIfNotHandled()?.let { mediaId ->
                navigateToMediaItem(mediaId)
            }
        }
    }

    private fun navigateToMediaItem(mediaId: String) {
        var fragment: MediaItemFragment? = getBrowseFragment(mediaId)
        if (fragment == null) {
            fragment = MediaItemFragment().apply {
                arguments = Bundle().apply {
                    putString(MEDIA_ID_ARG, mediaId)
                }
            }
            // If this is not the top level media (root), we add it to the fragment
            // back stack, so that actionbar toggle and Back will work appropriately:
            viewModel.showFragment(fragment, !isRootId(mediaId), mediaId)
        }
    }

    private fun isRootId(mediaId: String) = mediaId == viewModel.rootMediaItem.value?.mediaId

    private fun getBrowseFragment(mediaId: String): MediaItemFragment? {
        return supportFragmentManager.findFragmentByTag(mediaId) as? MediaItemFragment
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }
}
