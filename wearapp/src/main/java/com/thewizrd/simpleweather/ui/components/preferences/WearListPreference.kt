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
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Text
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.compose.tools.WearPreviewDevices

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

    WearPreference(
        title = title,
        subtitle = items.first { it.second == selectedItem }.first,
        onClick = {
            openDialog = true
        },
        modifier = modifier,
        enabled = enabled,
    )

    AlertDialog(
        visible = openDialog,
        onDismissRequest = {
            openDialog = false
        },
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center
            )
        },
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 52.dp),
    ) {
        items(
            items = items,
            key = { it.second }
        ) { item ->
            val checked = item.second == selectedItem

            RadioButton(
                modifier = Modifier.fillMaxWidth(),
                selected = checked,
                onSelect = {
                    onItemSelected(item.second)
                    openDialog = false
                },
                label = {
                    Text(text = item.first)
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
                colors = ButtonDefaults.buttonColors()
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(align = Alignment.Center),
                    painter = painterResource(id = sharedRes.drawable.ic_close_white_24dp),
                    contentDescription = stringResource(id = android.R.string.cancel)
                )
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