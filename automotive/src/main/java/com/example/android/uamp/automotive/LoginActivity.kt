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
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.android.uamp.common.MediaSessionConnection
import com.example.android.uamp.media.MusicService

/**
 * This class mocks the sign in flow for integration with MediaCenter in Android Automotive.
 *
 * This activity will be launched by Android Automotive MediaCenter via PendingIntent set in the
 * playback state in [MusicService.verifyLoginStatus].
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var signInButton: Button

    private lateinit var viewModel: LoginActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        inputEmail = findViewById(R.id.email)
        inputPassword = findViewById(R.id.password)
        signInButton = findViewById(R.id.sign_in_button)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProviders
            .of(this)
            .get(LoginActivityViewModel::class.java)

        viewModel.loggedIn.observe(this, Observer { loggedIn ->
            if (loggedIn == true) {
                Log.d(TAG, "Sign in successful")
                finish()
            }
        })

        signInButton.setOnClickListener {
            val email = inputEmail.text.toString()
            val password = inputPassword.text.toString()

            viewModel.login(email, password)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

/**
 * Basic ViewModel for [LoginActivity].
 */
class LoginActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val applicationContext = application.applicationContext
    private val mediaSessionConnection = MediaSessionConnection(
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
            mediaSessionConnection.sendCommand(LOGIN, loginParams) { resultCode, _ ->
                _loggedIn.postValue(resultCode == Activity.RESULT_OK)
            }
        }

    }
}


private const val TAG = "LoginActivity"
