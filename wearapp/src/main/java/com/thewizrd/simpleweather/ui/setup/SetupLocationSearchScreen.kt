package com.thewizrd.simpleweather.ui.setup

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices
import com.thewizrd.simpleweather.ui.navigation.Screen
import com.thewizrd.simpleweather.ui.utils.rememberFocusRequester

@Composable
fun SetupLocationSearchScreen(navController: NavController) {
    val context = LocalContext.current

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        activityResult.data?.let { data ->
            if (data.hasExtra(RecognizerIntent.EXTRA_RESULTS)) {
                // Result from voice input
                val voiceResults = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                if (!voiceResults.isNullOrEmpty()) {
                    val text = voiceResults[0]
                    if (!text.isNullOrEmpty()) {
                        navController.navigate(Screen.SetupLocationList.route + "?${Constants.KEY_SEARCH}=$text")
                    }
                }
            }
        }
    }

    val searchHintPrompt = stringResource(sharedRes.string.location_search_hint)

    SetupLocationSearchScreen(
        onVoiceSearch = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    searchHintPrompt
                )
            }

            voiceLauncher.launch(intent)
        },
        onTextSearch = { text ->
            if (!text.isNullOrEmpty()) {
                // If we're using search make sure gps feature is off
                if (settingsManager.useFollowGPS()) {
                    settingsManager.setFollowGPS(false)
                }

                navController.navigate(Screen.SetupLocationList.route + "?${Constants.KEY_SEARCH}=$text")
            }
        }
    )
}

@Composable
private fun SetupLocationSearchScreen(
    onVoiceSearch: () -> Unit = {},
    onTextSearch: (String?) -> Unit = {}
) {
    val localFocusMgr = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    ScreenScaffold { contentPadding ->
        var text by remember { mutableStateOf("") }
        val focusRequester = rememberFocusRequester()

        BasicTextField(
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusRequester.freeFocus()
                    localFocusMgr.clearFocus(force = true)
                },
                onSearch = {
                    onTextSearch(text)
                    focusRequester.freeFocus()
                    localFocusMgr.clearFocus(force = true)
                }
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
                showKeyboardOnFocus = true
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            ListHeader(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 14.dp, end = 14.dp,
                    top = 26.dp, bottom = 16.dp
                )
            ) {
                Text(text = stringResource(id = sharedRes.string.location_search_hint))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 10.dp)
                ) {
                    IconButton(
                        modifier = Modifier
                            .height(IconButtonDefaults.DefaultButtonSize)
                            .weight(1f, fill = true),
                        colors = IconButtonDefaults.filledIconButtonColors(),
                        onClick = onVoiceSearch
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard_voice_black_24dp),
                            contentDescription = stringResource(androidx.appcompat.R.string.abc_searchview_description_voice)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        modifier = Modifier
                            .height(IconButtonDefaults.DefaultButtonSize)
                            .weight(1f, fill = true),
                        colors = IconButtonDefaults.filledIconButtonColors(),
                        onClick = {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard_black_24dp),
                            contentDescription = stringResource(androidx.appcompat.R.string.abc_searchview_description_search)
                        )
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                text = ""
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun PreviewSetupLocationSearchScreen() {
    SetupLocationSearchScreen()
}