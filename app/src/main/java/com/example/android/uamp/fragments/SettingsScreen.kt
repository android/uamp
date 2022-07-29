package com.example.android.uamp.fragments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.uamp.R

@Composable
fun SettingsScreenDescription() {
    val mCheckedValue = remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.Center) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null)
                }
            },
            backgroundColor = Color.White

        )

        Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
            Row(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Spatial Audio", modifier = Modifier
                        .weight(2f), style = MaterialTheme.typography.subtitle1
                )
                Switch(
                    checked = mCheckedValue.value,
                    onCheckedChange = { mCheckedValue.value = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colorResource(id = R.color.colorPrimaryDark),
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray
                    ),
                )
            }
            SpatialAudioOutput()

        }
    }


//    val mCheckedState = remember { mutableStateOf(false) }
//
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
//            Text(text = "Enable Spatial Audio", modifier = Modifier.weight(2f))
//
//            Switch(checked = mCheckedState.value,
//                    onCheckedChange = { mCheckedState.value = it }
//            )
//        }
//        Text(text = "Spatial Audio Report: ")
//    }


}

@Composable
fun SpatialAudioOutput() {
    Column(verticalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = "Spatial Audio Output:",
            style = MaterialTheme.typography.subtitle2
        )
        Surface() {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                Row() {
                    Text(
                        text = "canBeSpatialized: ",
                        modifier = Modifier.weight(1f),
                        fontStyle = FontStyle.Italic
                    )
                    Text(text = "placeholder")
                }
                Row() {
                    Text(
                        text = "getImmersiveAudioLevel: ",
                        modifier = Modifier.weight(1f),
                        fontStyle = FontStyle.Italic
                    )
                    Text(text = "placeholder")
                }
                Row() {
                    Text(
                        text = "isAvailable: ",
                        modifier = Modifier.weight(1f),
                        fontStyle = FontStyle.Italic
                    )
                    Text(text = "placeholder")
                }
                Row() {
                    Text(
                        text = "isEnabled: ",
                        modifier = Modifier.weight(1f),
                        fontStyle = FontStyle.Italic
                    )
                    Text(text = "placeholder")
                }
                Row() {
                    Text(
                        text = "isHeadTrackerAvailable: ",
                        modifier = Modifier.weight(1f),
                        fontStyle = FontStyle.Italic
                    )
                    Text(text = "placeholder")
                }
            }
        }

    }

}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreenDescription()
}
