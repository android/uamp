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
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.android.uamp.automotive.databinding.PinSignInBinding

/**
 * Fragment that is used to facilitate PIN code sign-in. This fragment displayed a configurable
 * PIN code that users enter in a secondary device to perform sign-in.
 *
 *<p>This screen serves as a demo for UI best practices for PIN code sign in. Sign in implementation
 * will be app specific and is not included.
 */
class PinCodeSignInFragment : Fragment(R.layout.pin_sign_in) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        val binding = PinSignInBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.appIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.aural_logo))
        binding.primaryMessage.text = getString(R.string.pin_sign_in_primary_text)
        binding.secondaryMessage.text = getString(R.string.pin_sign_in_secondary_text)

        // Links in footer text should be clickable.
        binding.footer.text = HtmlCompat.fromHtml(
            context.getString(R.string.sign_in_footer),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.footer.movementMethod = LinkMovementMethod.getInstance()

        val pin = ViewModelProvider(requireActivity())
            .get(SignInActivityViewModel::class.java)
            .generatePin()

        // Remove existing PIN characters.
        if (binding.pinCodeContainer.childCount > 0) {
            binding.pinCodeContainer.removeAllViews()
        }

        for (element in pin) {
            val pinItem = LayoutInflater.from(context).inflate(
                R.layout.pin_item,
                binding.pinCodeContainer,
                false
            ) as TextView
            pinItem.text = element.toString()
            binding.pinCodeContainer.addView(pinItem)
        }
    }
}