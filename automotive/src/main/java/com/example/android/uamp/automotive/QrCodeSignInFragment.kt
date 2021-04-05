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
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.qr_sign_in.app_icon
import kotlinx.android.synthetic.main.qr_sign_in.footer
import kotlinx.android.synthetic.main.qr_sign_in.primary_message
import kotlinx.android.synthetic.main.qr_sign_in.qr_code
import kotlinx.android.synthetic.main.qr_sign_in.secondary_message
import kotlinx.android.synthetic.main.qr_sign_in.toolbar

/**
 * Fragment that is used to facilitate QR code sign-in. Users scan a QR code rendered by this
 * fragment with their phones, which performs the authentication required for sign-in
 *
 * <p>This screen serves as a demo for UI best practices for QR code sign in. Sign in implementation
 * will be app specific and is not included.
 */
class QrCodeSignInFragment : Fragment(R.layout.qr_sign_in) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        app_icon.setImageDrawable(getDrawable(requireContext(), R.drawable.aural_logo))
        primary_message.text = getString(R.string.qr_sign_in_primary_text)
        secondary_message.text = getString(R.string.qr_sign_in_secondary_text)

        // Links in footer text should be clickable.
        footer.text = HtmlCompat.fromHtml(
            context.getString(R.string.sign_in_footer),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        footer.movementMethod = LinkMovementMethod.getInstance()

        setQrCode(getString(R.string.qr_code_url))
    }

    /**
     * Sets the QR code rendered on this sign in screen.
     *
     * @param code The QR code to display.
     */
    private fun setQrCode(url: String) {
        Glide.with(this).load(url).into(qr_code)
    }
}
