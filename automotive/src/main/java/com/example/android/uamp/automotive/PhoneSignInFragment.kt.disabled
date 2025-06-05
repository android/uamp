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
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.example.android.uamp.automotive.databinding.PhoneSignInBinding

/**
 * Fragment that is used to facilitate phone sign-in. The fragment allows users to choose between
 * either the PIN or QR code sign-in flow.
 */
class PhoneSignInFragment : Fragment(R.layout.phone_sign_in) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        val binding = PhoneSignInBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Set up PIN sign in button.
        binding.appIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.aural_logo))
        binding.primaryMessage.text = getString(R.string.phone_sign_in_primary_text)
        binding.pinSignInButton.text = getString(R.string.pin_sign_in_button_label)
        binding.pinSignInButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.sign_in_container, PinCodeSignInFragment())
                .addToBackStack("landingPage")
                .commit()
        }

        // Set up QR code sign in button.
        binding.qrSignInButton.text = getString(R.string.qr_sign_in_button_label)
        binding.qrSignInButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.sign_in_container, QrCodeSignInFragment())
                .addToBackStack("landingPage")
                .commit()
        }

        // Links in footer text should be clickable.
        binding.footer.text = HtmlCompat.fromHtml(
            context.getString(R.string.sign_in_footer),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.footer.movementMethod = LinkMovementMethod.getInstance()
    }
}