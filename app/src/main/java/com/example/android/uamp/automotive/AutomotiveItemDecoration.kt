/*
 * Copyright 2024 Mixtape Player
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

package com.example.android.uamp.automotive

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * ItemDecoration for automotive RecyclerView to provide consistent spacing
 * between list items optimized for automotive UI guidelines.
 */
class AutomotiveItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        // Add top spacing for all items except the first
        if (position > 0) {
            outRect.top = spacing
        }

        // Add bottom spacing for all items except the last
        if (position < itemCount - 1) {
            outRect.bottom = spacing / 2
        }

        // Add horizontal padding for better visual appearance
        outRect.left = spacing
        outRect.right = spacing
    }
} 