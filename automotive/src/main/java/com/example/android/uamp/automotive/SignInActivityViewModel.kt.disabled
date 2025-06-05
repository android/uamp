/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.automotive

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.uamp.common.MusicServiceConnection
import java.util.Random

/**
 * Basic ViewModel for [SignInActivity].
 */
class SignInActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val applicationContext = application.applicationContext
    private val musicServiceConnection = MusicServiceConnection(
        applicationContext,
        ComponentName(applicationContext, AutomotiveMusicService::class.java)
    )

    private val _loggedIn = MutableLiveData<Boolean>()
    val loggedIn: LiveData<Boolean> = _loggedIn

    fun login(email: String, password: String) {
        if (TextUtils.isEmpty(email) or TextUtils.isEmpty(password)) {
            Toast.makeText(
                applicationContext,
                applicationContext.getString(R.string.missing_fields_error),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val loginParams = Bundle().apply {
                putString(LOGIN_EMAIL, email)
                putString(LOGIN_PASSWORD, password)
            }
            musicServiceConnection.sendCommand(LOGIN, loginParams) { resultCode, _ ->
                _loggedIn.postValue(resultCode == Activity.RESULT_OK)
            }
        }
    }

    fun generatePin(): CharSequence {
        return String.format("%08d", Random().nextInt(99999999))
    }
}