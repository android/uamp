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
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders

/**
 * Fragment that is used to facilitate PIN code sign-in. This fragment displayed a configurable
 * PIN code that users enter in a secondary device to perform sign-in.
 *
 *<p>This screen serves as a demo for UI best practices for PIN code sign in. Sign in implementation
 * will be app specific and is not included.
 */
class PinCodeSignInFragment : Fragment() {

    private lateinit var toolbar: Toolbar
    private lateinit var appIcon: ImageView
    private lateinit var primaryTextView: TextView
    private lateinit var secondaryTextView: TextView
    private lateinit var pinCodeContainer : ViewGroup
    private lateinit var footerTextView: TextView

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pin_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        toolbar = view.findViewById(R.id.toolbar)
        appIcon = view.findViewById(R.id.app_icon)
        primaryTextView = view.findViewById(R.id.primary_message)
        secondaryTextView = view.findViewById(R.id.secondary_message)
        pinCodeContainer = view.findViewById(R.id.pin_code_container)
        footerTextView = view.findViewById(R.id.footer)

        toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        appIcon.setImageDrawable(context.getDrawable(R.drawable.aural_logo))
        primaryTextView.text = getString(R.string.pin_sign_in_primary_text)
        secondaryTextView.text = getString(R.string.pin_sign_in_secondary_text)

        // Links in footer text should be clickable.
        footerTextView.text = HtmlCompat.fromHtml(context.getString(R.string.sign_in_footer),
                HtmlCompat.FROM_HTML_MODE_LEGACY)
        footerTextView.movementMethod = LinkMovementMethod.getInstance()

        val pin = ViewModelProviders.of(requireActivity())
                .get(SignInActivityViewModel::class.java)
                .generatePin()
        setPin(pin)
    }

    /**
     * Sets the PIN code rendered on this sign in screen.
     *
     * @param pin The PIN to display.
     */
    private fun setPin(pin: CharSequence) {
        // Remove existing PIN characters.
        if (pinCodeContainer.childCount > 0) {
            pinCodeContainer.removeAllViews()
        }

        for (element in pin) {
            val pinItem = LayoutInflater.from(context).inflate(
                    R.layout.pin_item,
                    pinCodeContainer,
                    false) as TextView
            pinItem.text = element.toString()
            pinCodeContainer.addView(pinItem)
        }
    }
}