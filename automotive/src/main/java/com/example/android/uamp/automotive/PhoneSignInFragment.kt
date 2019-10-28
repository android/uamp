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

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment

/**
 * Fragment that is used to facilitate phone sign-in. The fragment allows users to choose between
 * either the PIN or QR code sign-in flow.
 */
class PhoneSignInFragment : Fragment() {

    private lateinit var toolbar: Toolbar
    private lateinit var appIcon: ImageView
    private lateinit var primaryTextView: TextView
    private lateinit var pinSignInButton: Button
    private lateinit var qrCodeSignInButton : Button
    private lateinit var footerTextView: TextView

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.phone_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        toolbar = view.findViewById(R.id.toolbar)
        appIcon = view.findViewById(R.id.app_icon)
        primaryTextView = view.findViewById(R.id.primary_message)
        pinSignInButton = view.findViewById(R.id.pin_sign_in_button)
        qrCodeSignInButton = view.findViewById(R.id.qr_sign_in_button)
        footerTextView = view.findViewById(R.id.footer)

        toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Set up PIN sign in button.
        appIcon.setImageDrawable(context.getDrawable(R.drawable.aural_logo))
        primaryTextView.text = getString(R.string.phone_sign_in_primary_text)
        pinSignInButton.text = getString(R.string.pin_sign_in_button_label)
        pinSignInButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.sign_in_container, PinCodeSignInFragment())
                    .addToBackStack("landingPage")
                    .commit()
        }

        // Set up QR code sign in button.
        qrCodeSignInButton.text = getString(R.string.qr_sign_in_button_label)
        qrCodeSignInButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.sign_in_container, QrCodeSignInFragment())
                    .addToBackStack("landingPage")
                    .commit()
        }

        // Links in footer text should be clickable.
        footerTextView.text = HtmlCompat.fromHtml(context.getString(R.string.sign_in_footer),
                HtmlCompat.FROM_HTML_MODE_LEGACY)
        footerTextView.movementMethod = LinkMovementMethod.getInstance()
    }
}