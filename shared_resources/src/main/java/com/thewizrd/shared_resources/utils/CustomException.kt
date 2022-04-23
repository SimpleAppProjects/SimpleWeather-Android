package com.thewizrd.shared_resources.utils

import androidx.annotation.StringRes
import com.thewizrd.shared_resources.sharedDeps

class CustomException(@StringRes stringResId: Int) : Exception() {
    @StringRes
    var stringResId = Int.MIN_VALUE

    init {
        this.stringResId = stringResId
    }

    override val message: String?
        get() {
            if (stringResId != Int.MIN_VALUE) {
                return sharedDeps.context.getString(stringResId)
            }

            return super.message
        }
}