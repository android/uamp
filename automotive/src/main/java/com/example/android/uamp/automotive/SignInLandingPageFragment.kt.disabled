/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.automotive

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.View.AUTOFILL_HINT_USERNAME
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

const val RC_SIGN_IN = 9001
const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

// Control the supported sign in flows by toggling the constants below.
const val ENABLE_PIN_SIGN_IN = true
const val ENABLE_QR_CODE_SIGN_IN = true
const val ENABLE_GOOGLE_SIGN_IN = true
const val ENABLE_USERNAME_PASSWORD_SIGN_IN = true

/**
 * A fragment that renders the landing screen for a sign-in flow. This screen can be configured
 * to display third-party sign-in, PIN sign-in, QR-code sign-in and/or Google sign-in.
 */
class SignInLandingPageFragment : Fragment() {

    companion object {
        internal const val CAR_SIGN_IN_IDENTIFIER_KEY = "userID"
    }

    private lateinit var toolbar: Toolbar
    private lateinit var appIcon: ImageView
    private lateinit var phoneSignInButton: Button
    private lateinit var googleSignInButton: Button
    private lateinit var usernameAndPasswordSignInButton: Button
    private lateinit var primaryTextView: TextView
    private lateinit var identifierContainer: TextInputLayout
    private lateinit var identifierInput: TextInputEditText
    private lateinit var footerTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = if (ENABLE_USERNAME_PASSWORD_SIGN_IN)
            R.layout.sign_in_landing_page_with_username_and_password
        else R.layout.sign_in_landing_page

        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        toolbar = view.findViewById(R.id.toolbar)
        appIcon = view.findViewById(R.id.app_icon)
        primaryTextView = view.findViewById(R.id.primary_message)
        footerTextView = view.findViewById(R.id.footer)
        phoneSignInButton = view.findViewById(R.id.phone_sign_in_button)
        googleSignInButton = view.findViewById(R.id.google_sign_in_button)

        if (ENABLE_USERNAME_PASSWORD_SIGN_IN) {
            usernameAndPasswordSignInButton = view.findViewById(R.id.sign_in_button)
            identifierContainer = view.findViewById(R.id.identifier_container)
            identifierInput = view.findViewById(R.id.identifier_input)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                identifierInput.setAutofillHints(AUTOFILL_HINT_USERNAME)
            }
        }

        toolbar.setNavigationOnClickListener { requireActivity().finish() }

        appIcon.setImageDrawable(context.getDrawable(R.drawable.aural_logo))
        primaryTextView.text = getString(R.string.sign_in_primary_text)

        // Links in footer text should be clickable.
        footerTextView.text = HtmlCompat.fromHtml(
            context.getString(R.string.sign_in_footer),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        footerTextView.movementMethod = LinkMovementMethod.getInstance()

        configureUsernameAndPasswordSignIn()
        configurePhoneSignIn()
        configureGoogleSignIn()
    }

    private fun configureUsernameAndPasswordSignIn() {
        if (!ENABLE_USERNAME_PASSWORD_SIGN_IN) {
            return
        }

        identifierContainer.hint = getString(R.string.sign_in_user_id_hint)
        identifierInput.inputType = InputType.TYPE_CLASS_TEXT

        usernameAndPasswordSignInButton.text = getString(R.string.sign_in_next_button_label)
        usernameAndPasswordSignInButton.setOnClickListener {
            val identifier = identifierInput.text
            if (TextUtils.isEmpty(identifier)) {
                identifierInput.error = getString(R.string.sign_in_username_error)
            } else {
                val args = Bundle()
                args.putString(CAR_SIGN_IN_IDENTIFIER_KEY, identifierInput.text.toString())
                val fragment = UsernameAndPasswordSignInFragment()
                fragment.arguments = args

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.sign_in_container, fragment)
                    .addToBackStack("landingPage")
                    .commit()
            }
        }
    }

    private fun configurePhoneSignIn() {
        if (!ENABLE_QR_CODE_SIGN_IN && !ENABLE_PIN_SIGN_IN) {
            phoneSignInButton.visibility = View.GONE
            return
        }

        lateinit var phoneSignInFragment: Fragment

        if (ENABLE_QR_CODE_SIGN_IN && ENABLE_PIN_SIGN_IN) {
            // Reduce the number of choices displayed to the user in a single screen. If both PIN
            // and QR code sign in is enabled, separate the choice between the two options to a
            // new screen.
            phoneSignInFragment = PhoneSignInFragment()
        } else if (ENABLE_PIN_SIGN_IN) {
            phoneSignInFragment = PinCodeSignInFragment()
        } else if (ENABLE_QR_CODE_SIGN_IN) {
            phoneSignInFragment = QrCodeSignInFragment()
        }

        phoneSignInButton.text = getString(R.string.phone_sign_in_button_label)
        phoneSignInButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.sign_in_container, phoneSignInFragment)
                .addToBackStack("landingPage")
                .commit()
        }
    }

    /**
     * Configure the Google sign in option on the landing page.
     *
     * <p>https://developers.google.com/identity/sign-in/android/start provides additional
     * information on integrating Google sign in into your Android app.
     */
    private fun configureGoogleSignIn() {
        if (!ENABLE_GOOGLE_SIGN_IN or !checkPlayServices()) {
            googleSignInButton.visibility = View.GONE
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()

        googleSignInButton.text = getString(R.string.google_sign_in_button_label)

        googleSignInButton.setOnClickListener {
            val mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance();
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(
                    activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST
                ).show();
            }
            return false;
        }
        return true;
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignIn(task)
        }
    }

    private fun handleGoogleSignIn(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            @Suppress("unused_variable") val idToken = account?.idToken

            // Send ID Token to server and validate.

        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Toast.makeText(
                requireContext(), getString(R.string.sign_in_failed_message, e.statusCode),
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }
}