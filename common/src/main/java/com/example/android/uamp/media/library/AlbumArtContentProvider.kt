/*
 * Copyright 2019 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.media.library

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

// The amount of time to wait for the album art file to download before timing out.
const val DOWNLOAD_TIMEOUT_SECONDS = 30L

internal class AlbumArtContentProvider : ContentProvider() {

    companion object {
        private val uriMap = mutableMapOf<Uri, Uri>()

        fun mapUri(uri: Uri): Uri {
            val path = uri.encodedPath?.substring(1)?.replace('/', ':') ?: return Uri.EMPTY
            val contentUri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("com.example.android.uamp")
                .path(path)
                .build()
            uriMap[contentUri] = uri
            return contentUri
        }
    }

    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context = this.context ?: return null
        val remoteUri = uriMap[uri] ?: throw FileNotFoundException(uri.path)

        var file = File(context.cacheDir, uri.path)

        if (!file.exists()) {
            // Use Glide to download the album art.
            val cacheFile = Glide.with(context)
                .asFile()
                .load(remoteUri)
                .submit()
                .get(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            // Rename the file Glide created to match our own scheme.
            cacheFile.renameTo(file)

            file = cacheFile
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ) = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0

    override fun getType(uri: Uri): String? = null

}
