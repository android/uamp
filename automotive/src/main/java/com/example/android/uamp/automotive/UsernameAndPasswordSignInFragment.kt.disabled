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

import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Fragment that is used to facilitates username and password sign-in.
 */
class UsernameAndPasswordSignInFragment : Fragment() {

    private lateinit var toolbar: Toolbar
    private lateinit var appIcon: ImageView
    private lateinit var primaryTextView: TextView
    private lateinit var passwordContainer: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var submitButton: Button
    private lateinit var footerTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.username_and_password_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        toolbar = view.findViewById(R.id.toolbar)
        appIcon = view.findViewById(R.id.app_icon)
        primaryTextView = view.findViewById(R.id.primary_message)
        passwordContainer = view.findViewById(R.id.password_container)
        passwordInput = view.findViewById(R.id.password_input)
        submitButton = view.findViewById(R.id.submit_button)
        footerTextView = view.findViewById(R.id.footer)

        toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        appIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.aural_logo))
        primaryTextView.text = getString(R.string.username_and_password_sign_in_primary_text)
        passwordContainer.hint = getString(R.string.password_hint)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            passwordInput.setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
        }

        // Links in footer text should be clickable.
        footerTextView.text = HtmlCompat.fromHtml(
            context.getString(R.string.sign_in_footer),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        footerTextView.movementMethod = LinkMovementMethod.getInstance()

        // Get user identifier from previous screen.
        val userId = arguments?.getString(SignInLandingPageFragment.CAR_SIGN_IN_IDENTIFIER_KEY)

        submitButton.text = getString(R.string.sign_in_submit_button_label)
        submitButton.setOnClickListener {
            onSignIn(userId!!, passwordInput.text.toString())
        }
    }

    private fun onSignIn(userIdentifier: CharSequence, password: CharSequence) {
        ViewModelProvider(requireActivity())
            .get(SignInActivityViewModel::class.java)
            .login(userIdentifier.toString(), password.toString())
    }
}