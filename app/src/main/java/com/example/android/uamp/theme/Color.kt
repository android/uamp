package com.example.android.uamp.theme

import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color

val Red400 = Color(0XEF534E)
val RedLight = Color(0XFF867A)
val RedDark = Color(0xB61825)

val secondaryRed = Color(0xEF534F)
val secondaryRedDark = Color(0XB61826)


var UAMPColors = darkColors(
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


