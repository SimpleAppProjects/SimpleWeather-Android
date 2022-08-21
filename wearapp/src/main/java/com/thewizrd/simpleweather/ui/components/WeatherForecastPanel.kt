package com.thewizrd.simpleweather.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.thewizrd.common.controls.ForecastItemViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.text.spannableStringToAnnotatedString

@Composable
fun WeatherForecastPanel(
    model: ForecastItemViewModel
) {
    val popData = remember(model.extras) {
        model.extras?.get(WeatherDetailsType.POPCHANCE)
    }
    val windData = remember(model.extras) {
        model.extras?.get(WeatherDetailsType.WINDSPEED)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.list_item_padding),
                end = dimensionResource(R.dimen.list_item_padding),
                top = 4.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                textAlign = TextAlign.Center,
                text = model.date ?: WeatherIcons.EM_DASH,
                style = MaterialTheme.typography.body1
            )
            WeatherIcon(
                modifier = Modifier.size(
                    width = 32.dp, height = 36.dp
                ),
                weatherIcon = model.weatherIcon,
                shouldAnimate = true
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = model.hiTemp ?: WeatherIcons.PLACEHOLDER,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = painterResource(id = R.drawable.wi_direction_up),
                        tint = Color(0xFFFF4500),
                        contentDescription = null
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = model.loTemp ?: WeatherIcons.PLACEHOLDER,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = painterResource(id = R.drawable.wi_direction_down),
                        tint = Color(0xFF87CEFA),
                        contentDescription = null
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (popData != null) {
                Row {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 2.dp),
                        painter = painterResource(R.drawable.wi_umbrella),
                        tint = colorResource(R.color.colorPrimaryLight),
                        contentDescription = null
                    )
                    Text(
                        text = spannableStringToAnnotatedString(popData.value),
                        style = MaterialTheme.typography.body1.copy(fontSize = 14.sp),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        color = colorResource(R.color.colorPrimaryLight)
                    )
                }
            }
            if (popData != null && windData != null) {
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (windData != null) {
                Row {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 2.dp)
                            .rotate(windData.iconRotation.toFloat()),
                        painter = painterResource(R.drawable.wi_wind_direction),
                        tint = Color(0xFF20B2AA),
                        contentDescription = null
                    )
                    Text(
                        text = spannableStringToAnnotatedString(windData.value),
                        style = MaterialTheme.typography.body1.copy(fontSize = 14.sp),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        color = Color(0xFF20B2AA)
                    )
                }
            }
        }
    }
}