package com.thewizrd.simpleweather.ui.components.preferences

import android.app.Activity.RESULT_OK
import android.app.RemoteInput
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.thewizrd.shared_resources.Constants
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.helpers.SimpleActionModeCallback
import com.thewizrd.simpleweather.ui.text.toAnnotatedString
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices
import com.thewizrd.simpleweather.utils.hideInputMethod

@Composable
fun WearEditTextPreference(
    modifier: Modifier = Modifier,
    title: String,
    text: CharSequence,
    onTextChanged: (CharSequence) -> Unit,
    enabled: Boolean = true,
) {
    var openDialog by remember { mutableStateOf(false) }
    var textForInput by rememberSaveable { mutableStateOf(text) }
    val scrollState = rememberScalingLazyListState()

    WearPreference(
        title = title,
        subtitle = text.toAnnotatedString(),
        onClick = {
            openDialog = true
        },
        modifier = modifier,
        enabled = enabled,
    )

    Dialog(
        showDialog = openDialog,
        onDismissRequest = {
            openDialog = false
        },
        scrollState = scrollState
    ) {
        val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    RESULT_OK -> {
                        it.data?.let { data ->
                            val results = RemoteInput.getResultsFromIntent(data)
                            textForInput = results?.getCharSequence(Constants.KEY_SEARCH) ?: ""
                            openDialog = false
                        }
                    }
                }
            }

        Alert(
            title = {
                Text(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    text = title,
                    textAlign = TextAlign.Center
                )
            },
            scrollState = scrollState,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 32.dp),
            negativeButton = {
                Button(
                    onClick = {
                        openDialog = false
                    },
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .wrapContentSize(align = Alignment.Center),
                        painter = painterResource(id = R.drawable.ic_close_white_24dp),
                        contentDescription = stringResource(id = android.R.string.cancel)
                    )
                }
            },
            positiveButton = {
                Button(
                    onClick = {
                        onTextChanged(textForInput)
                        openDialog = false
                    },
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .wrapContentSize(align = Alignment.Center),
                        painter = painterResource(id = R.drawable.ic_check_24dp),
                        contentDescription = stringResource(id = android.R.string.ok)
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = 16.dp)
                    .border(1.dp, MaterialTheme.colors.primary, RoundedCornerShape(8.dp)),
            ) {
                AndroidView(
                    modifier = Modifier
                        .padding(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        )
                        .fillMaxWidth(),
                    factory = { context ->
                        val view =
                            LayoutInflater.from(context).inflate(R.layout.edit_text_layout, null)
                        val editText = view as EditText
                        editText.apply {
                            SimpleActionModeCallback().also {
                                customSelectionActionModeCallback = it
                                customInsertionActionModeCallback = it
                            }
                        }
                    }
                ) { editText ->
                    editText.setOnEditorActionListener { v, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                            textForInput = v.text
                            onTextChanged(v.text)
                            v.hideInputMethod()
                            openDialog = false
                            return@setOnEditorActionListener true
                        }
                        false
                    }
                    editText.setText(textForInput)
                    editText.setSelection(textForInput.length)
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun WearEditTextPreferencePreview() {
    var textInput: CharSequence by remember { mutableStateOf("Edit Text") }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        WearEditTextPreference(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .wrapContentHeight(),
            title = "EditText Preference Title",
            text = textInput,
            onTextChanged = {
                textInput = it
            }
        )
    }
}