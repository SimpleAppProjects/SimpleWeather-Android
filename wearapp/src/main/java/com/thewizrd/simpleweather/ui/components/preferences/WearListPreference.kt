package com.thewizrd.simpleweather.ui.components.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices

@Composable
fun <T : Any> WearListPreference(
    modifier: Modifier = Modifier,
    title: String,
    items: List<Pair<String, T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    enabled: Boolean = true,
) {
    var openDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScalingLazyListState()

    WearPreference(
        title = title,
        subtitle = items.first { it.second == selectedItem }.first,
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
        Alert(
            title = {
                Text(
                    text = title,
                    textAlign = TextAlign.Center
                )
            },
            scrollState = scrollState,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 52.dp),
        ) {
            items(
                items = items,
                key = { it.second }
            ) { item ->
                val checked = item.second == selectedItem

                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    checked = checked,
                    onCheckedChange = {
                        if (it) {
                            onItemSelected(item.second)
                        }
                        openDialog = false
                    },
                    label = {
                        Text(text = item.first)
                    },
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.radioIcon(checked = checked),
                            contentDescription = if (checked) "Checked" else "Unchecked"
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Button(
                    onClick = {
                        openDialog = false
                    },
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .wrapContentSize(align = Alignment.Center),
                        painter = painterResource(id = R.drawable.ic_close_white_24dp),
                        contentDescription = stringResource(id = android.R.string.cancel)
                    )
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun WearListPreferencePreview() {
    val options = listOf(
        "One" to "1",
        "Two" to "2",
        "Three" to "3",
    )
    var selectedTheme by remember { mutableStateOf(value = options.first()) }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        WearListPreference(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .wrapContentHeight(),
            title = "List Preference Title",
            items = options,
            selectedItem = selectedTheme.second,
            onItemSelected = { selected ->
                selectedTheme = options.first { it.second == selected }
            },
        )
    }
}