package com.thewizrd.simpleweather.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.node.Ref
import com.thewizrd.simpleweather.BuildConfig
import timber.log.Timber

// Note the inline function below which ensures that this function is essentially
// copied at the call site to ensure that its logging only recompositions from the
// original call site.
@Composable
inline fun LogCompositions(tag: String, msg: String) {
    if (BuildConfig.DEBUG) {
        val ref = remember { Ref<Int>().apply { value = 0 } }
        SideEffect { ref.value = ref.value?.plus(1) }
        Timber.tag(tag).d("Compositions: $msg ${ref.value}")
    }
}