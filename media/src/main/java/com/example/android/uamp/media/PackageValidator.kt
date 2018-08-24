package com.example.android.uamp.media

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.os.Build
import android.os.Process
import android.support.annotation.XmlRes
import android.support.v4.media.MediaBrowserServiceCompat
import android.util.Base64
import android.util.Log
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Validates that the calling package is authorized to browse a [MediaBrowserServiceCompat].
 *
 * The list of allowed signing certificates and their corresponding package names is defined in
 * res/xml/allowed_media_browser_callers.xml.
 *
 * If you want to add a new caller to allowed_media_browser_callers.xml and you don't know
 * its signature, this class will print to logcat (INFO level) a message with the proper
 * xml tags to add to allow the caller.
 *
 * For more information, see res/xml/allowed_media_browser_callers.xml.
 */
class PackageValidator(context: Context, @XmlRes xmlResId: Int) {
    private val context: Context
    private val packageManager: PackageManager

    private val validCertificates: Map<String, CallerInfo>
    private val platformSignature: String

    private val callerChecked = mutableMapOf<String, Pair<Int, Boolean>>()

    init {
        val parser = context.resources.getXml(xmlResId)
        this.context = context.applicationContext
        this.packageManager = this.context.packageManager

        validCertificates = readValidCertificates(parser)
        platformSignature = getSystemSignature()
    }

    fun isCallerAllowed(callingPackage: String, callingUid: Int): Boolean {
        // If the caller has already been checked, return the previous result here.
        val (checkedUid, checkResult) = callerChecked[callingPackage] ?: Pair(0, false)
        if (checkedUid == callingUid) {
            return checkResult
        }

        // Verify that things aren't ... broken. (This test should always pass.)
        if (lookupUid(callingPackage) != callingUid) {
            throw IllegalStateException("Caller's package UID doesn't match caller's actual UID?")
        }

        /**
         * Because some of these checks can be slow, we save the results in [callerChecked] after
         * this code is run.
         *
         * In particular, there's little reason to recompute the calling package's certificate
         * signature (SHA-256) each call.
         *
         * This is safe to do as we know the UID matches the package's UID (from the check above),
         * and app UIDs are set at install time. Additionally, a package name + UID is guarenteed to
         * be constant until a reboot. (After a reboot then a previously assigned UID could be
         * reassigned.)
         */
        val callerPermitted =
                if (Process.SYSTEM_UID == callingUid || Process.myUid() == callingUid) {
                    // Always allow calls from the framework and ourself.
                    true
                } else {
                    val signature = lookupSignature(callingPackage)
                    val matchingSignature = validCertificates[callingPackage]?.signatures?.first {
                        it.signature == signature
                    }
                    val allowed = matchingSignature != null || signature == platformSignature

                    if (!allowed && signature != null) {
                        logUnknownCaller(callingPackage, signature)
                    }
                    allowed
                }

        // Save our work for next time.
        callerChecked[callingPackage] = Pair(callingUid, callerPermitted)
        return callerPermitted
    }

    /**
     * Logs an info level message with details of how to add a caller to the allowed callers list
     * when the app is debuggable.
     */
    private fun logUnknownCaller(callingPackage: String, signature: String) {
        val appName = packageManager.run {
            val packageInfo = getPackageInfo(callingPackage, 0)
            packageInfo.applicationInfo.loadLabel(this).toString()
        }

        if (BuildConfig.DEBUG) {
            Log.i(TAG,
                    context.getString(
                            R.string.allowed_caller_log, appName, callingPackage, signature))
        }
    }

    /**
     * Looks up the user ID (UID) of an app with a given package name.
     */
    private fun lookupUid(packageName: String): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                packageManager.getPackageUid(packageName, 0)
            } else {
                packageManager.getApplicationInfo(packageName, 0).uid
            }

    /**
     * Looks up the signature of a given package.
     *
     * The "signature" is a SHA-256 hash of the public key of the signing certificate used by
     * the app.
     *
     * If the app is not found, or if the app does not have exactly one signature, this method
     * returns `null` as the signature.
     */
    private fun lookupSignature(callingPackage: String): String? {
        val packageInfo = getPackageInfo(callingPackage) ?: return null

        // Security best practices dictate that an app should be signed with exactly one (1)
        // signature. Because of this, if there are multiple signatures, reject it.
        if (packageInfo.signatures == null || packageInfo.signatures.size != 1) {
            return null
        } else {
            val certificate = packageInfo.signatures[0].toByteArray()
            return getSignatureSha256(certificate)
        }
    }

    /**
     * @return [PackageInfo] for the package name or null if it's not found.
     */
    @SuppressLint("PackageManagerGetSignatures")
    private fun getPackageInfo(callingPackage: String): PackageInfo? =
            packageManager.getPackageInfo(callingPackage, PackageManager.GET_SIGNATURES)

    private fun readValidCertificates(parser: XmlResourceParser): Map<String, CallerInfo> {

        val validCertificates = LinkedHashMap<String, CallerInfo>()
        try {
            var eventType = parser.next()
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG) {
                    val callerInfo = when (parser.name) {
                        "signing_certificate" -> parseV1Tag(parser)
                        "signature" -> parseV2Tag(parser)
                        else -> null
                    }

                    callerInfo?.let { info ->
                        val packageName = info.packageName
                        val existingCallerInfo = validCertificates[packageName]
                        if (existingCallerInfo != null) {
                            existingCallerInfo.signatures += callerInfo.signatures
                        } else {
                            validCertificates[packageName] = callerInfo
                        }
                    }
                }

                eventType = parser.next()
            }
        } catch (xmlException: XmlPullParserException) {
            Log.e(TAG, "Could not read allowed callers from XML.", xmlException)
        } catch (ioException: IOException) {
            Log.e(TAG, "Could not read allowed callers from XML.", ioException)
        }

        return validCertificates
    }

    /**
     * Parses a v1 format tag. See allowed_media_browser_callers.xml for more details.
     */
    private fun parseV1Tag(parser: XmlResourceParser): CallerInfo {
        val name = parser.getAttributeValue(null, "name")
        val packageName = parser.getAttributeValue(null, "package")
        val isRelease = parser.getAttributeBooleanValue(null, "release", false)
        val certificate = parser.nextText().replace(WHITESPACE_REGEX, "")
        val signature = getSignatureSha256(certificate)

        val callerSignature = CallerSignature(signature, isRelease)
        return CallerInfo(name, packageName, mutableSetOf(callerSignature))
    }

    /**
     * Parses a v2 format tag. See allowed_media_browser_callers.xml for more details.
     */
    private fun parseV2Tag(parser: XmlResourceParser): CallerInfo {
        val name = parser.getAttributeValue(null, "name")
        val packageName = parser.getAttributeValue(null, "package")

        val callerSignatures = mutableSetOf<CallerSignature>()
        var eventType = parser.next()
        while (eventType != XmlResourceParser.END_TAG) {
            val isRelease = parser.getAttributeBooleanValue(null, "release", false)
            val signature = parser.nextText().replace(WHITESPACE_REGEX, "").toLowerCase()
            callerSignatures += CallerSignature(signature, isRelease)

            eventType = parser.next()
        }

        return CallerInfo(name, packageName, callerSignatures)
    }

    /**
     * Finds the Android platform signing key signature. This key is never null.
     */
    private fun getSystemSignature(): String = lookupSignature(ANDROID_PLATFORM)
            ?: throw IllegalStateException("Platform signature not found")

    /**
     * Creates a SHA-256 signature given a Base64 encoded certificate.
     */
    private fun getSignatureSha256(certificate: String): String {
        return getSignatureSha256(Base64.decode(certificate, Base64.DEFAULT))
    }

    /**
     * Creates a SHA-256 signature given a certificate byte array.
     */
    private fun getSignatureSha256(certificate: ByteArray): String {
        val md: MessageDigest
        try {
            md = MessageDigest.getInstance("SHA256")
        } catch (noSuchAlgorithmException: NoSuchAlgorithmException) {
            Log.e(TAG, "No such algorithm: $noSuchAlgorithmException")
            throw RuntimeException("Could not find SHA256 hash algorithm", noSuchAlgorithmException)
        }
        md.update(certificate)

        // This code takes the byte array generated by `md.digest()` and joins each of the bytes
        // to a string, applying the string format `%02x` on each digit before it's appended, with
        // a colon (':') between each of the items.
        // For example: input=[0,2,4,6,8,10,12], output="00:02:04:06:08:0a:0c"
        return md.digest().joinToString(":") { String.format("%02x", it) }
    }

    private data class CallerInfo(
            internal val name: String,
            internal val packageName: String,
            internal val signatures: MutableSet<CallerSignature>
    )

    private data class CallerSignature(
            internal val signature: String,
            internal val release: Boolean
    )
}

private const val TAG = "PackageValidator"
private const val ANDROID_PLATFORM = "android"
private val WHITESPACE_REGEX = "\\s|\\n".toRegex()
