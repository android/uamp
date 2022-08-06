/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Define custom theme for UAMP
 */
@Composable
fun UAMPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {


    val darkThemeColors = darkColors(
        primary = RedDark,
        primaryVariant = RedLight,
        onPrimary = Color.White,
        secondary = secondaryRedDark,
        onSecondary = Color.White,
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        error = Color.Red,
        onError = Color.Black
    )

    val lightThemeColors = lightColors(
        primary = Red400,
        primaryVariant = RedLight,
        onPrimary = Color.Black,
        secondary = secondaryRed,
        onSecondary = Color.Black,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        error = Color.Red,
        onError = Color.White
    )

    val colors = if (darkTheme) darkThemeColors else lightThemeColors

    MaterialTheme(
        colors = colors,
        typography = UAMPTypography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
