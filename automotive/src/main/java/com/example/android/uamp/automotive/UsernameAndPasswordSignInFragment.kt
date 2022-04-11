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
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.android.uamp.automotive.databinding.PhoneSignInBinding
import com.example.android.uamp.automotive.databinding.UsernameAndPasswordSignInBinding

/**
 * Fragment that is used to facilitates username and password sign-in.
 */
class UsernameAndPasswordSignInFragment : Fragment(R.layout.username_and_password_sign_in) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        val binding = UsernameAndPasswordSignInBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.appIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.aural_logo))
        binding.primaryMessage.text = getString(R.string.username_and_password_sign_in_primary_text)
        binding.passwordContainer.hint = getString(R.string.password_hint)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.passwordInput.setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
        }

        // Links in footer text should be clickable.
        binding.footer.text = HtmlCompat.fromHtml(
            context.getString(R.string.sign_in_footer),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.footer.movementMethod = LinkMovementMethod.getInstance()

        // Get user identifier from previous screen.
        val userId = arguments?.getString(SignInLandingPageFragment.CAR_SIGN_IN_IDENTIFIER_KEY)

        binding.submitButton.text = getString(R.string.sign_in_submit_button_label)
        binding.submitButton.setOnClickListener {
            onSignIn(userId!!, binding.passwordInput.text.toString())
        }
    }

    private fun onSignIn(userIdentifier: CharSequence, password: CharSequence) {
        ViewModelProvider(requireActivity())
            .get(SignInActivityViewModel::class.java)
            .login(userIdentifier.toString(), password.toString())
    }
}
