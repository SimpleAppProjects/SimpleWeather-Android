package com.thewizrd.simpleweather.fragments

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

public class WearDialogParams private constructor() {
    var mTitle: CharSequence? = null
    var mMessage: CharSequence? = null
    var mIcon: Drawable? = null

    var mShowPositiveButton = true
    var mShowNegativeButton = true

    var mOnPositiveButtonClicked: WearDialogInterface.OnClickListener? = null
    var mOnNegativeButtonClicked: WearDialogInterface.OnClickListener? = null
    var mOnDismissListener: View.OnClickListener? = null

    var mItems: List<CharSequence>? = null

    var mIsSingleChoice = false
    var mCheckedItem = -1
    var mOnClickListener: WearDialogInterface.OnClickListener? = null

    var mIsMultiChoice = false
    var mCheckedItems: List<Boolean>? = null
    var mOnCheckboxClickListener: WearDialogInterface.OnMultiChoiceClickListener? = null

    var mContentViewLayoutResId: Int = 0
    var mContentView: View? = null

    public class Builder(private val context: Context) {
        private val params = WearDialogParams()

        public fun setTitle(@StringRes titleId: Int): Builder {
            params.mTitle = context.getString(titleId)
            return this
        }

        public fun setTitle(title: CharSequence?): Builder {
            params.mTitle = title
            return this
        }

        public fun setMessage(@StringRes messageId: Int): Builder {
            params.mMessage = context.getString(messageId)
            return this
        }

        public fun setMessage(message: CharSequence?): Builder {
            params.mMessage = message
            return this
        }

        public fun setIcon(@DrawableRes iconId: Int): Builder {
            params.mIcon = ContextCompat.getDrawable(context, iconId)
            return this
        }

        public fun setIcon(icon: Drawable?): Builder {
            params.mIcon = icon
            return this
        }

        public fun hidePositiveButton(): Builder {
            params.mShowPositiveButton = false
            return this
        }

        public fun hideNegativeButton(): Builder {
            params.mShowNegativeButton = false
            return this
        }

        public fun setOnPositiveButtonClicked(listener: WearDialogInterface.OnClickListener?): Builder {
            params.mOnPositiveButtonClicked = listener
            return this
        }

        public fun setOnNegativeButtonClicked(listener: WearDialogInterface.OnClickListener?): Builder {
            params.mOnNegativeButtonClicked = listener
            return this
        }

        public fun setOnDismissListener(listener: View.OnClickListener?): Builder {
            params.mOnDismissListener = listener
            return this
        }

        public fun setItems(
            @ArrayRes itemsId: Int,
            listener: WearDialogInterface.OnClickListener?
        ): Builder {
            params.mItems = context.resources.getTextArray(itemsId).toList()
            params.mOnClickListener = listener
            return this
        }

        public fun setItems(
            items: List<CharSequence>?,
            listener: WearDialogInterface.OnClickListener?
        ): Builder {
            params.mItems = items?.toList()
            params.mOnClickListener = listener
            return this
        }

        public fun setMultiChoiceItems(
            @ArrayRes itemsId: Int,
            checkedItems: List<Boolean>?,
            listener: WearDialogInterface.OnMultiChoiceClickListener?
        ): Builder {
            params.mItems = context.resources.getTextArray(itemsId).toList()
            params.mOnCheckboxClickListener = listener
            params.mCheckedItems = checkedItems
            params.mIsMultiChoice = true
            return this
        }

        public fun setMultiChoiceItems(
            items: List<CharSequence>?,
            checkedItems: List<Boolean>?,
            listener: WearDialogInterface.OnMultiChoiceClickListener?
        ): Builder {
            params.mItems = items?.toList()
            params.mOnCheckboxClickListener = listener
            params.mCheckedItems = checkedItems
            params.mIsMultiChoice = true
            return this
        }

        public fun setSingleChoiceItems(
            @ArrayRes itemsId: Int,
            checkedItem: Int,
            listener: WearDialogInterface.OnClickListener?
        ): Builder {
            params.mItems = context.resources.getTextArray(itemsId).toList()
            params.mOnClickListener = listener
            params.mCheckedItem = checkedItem
            params.mIsSingleChoice = true
            return this
        }

        public fun setSingleChoiceItems(
            items: List<CharSequence>?,
            checkedItem: Int,
            listener: WearDialogInterface.OnClickListener?
        ): Builder {
            params.mItems = items?.toList()
            params.mOnClickListener = listener
            params.mCheckedItem = checkedItem
            params.mIsSingleChoice = true
            return this
        }

        public fun setView(layoutResId: Int): Builder {
            params.mContentView = null
            params.mContentViewLayoutResId = layoutResId
            return this
        }

        public fun setView(view: View?): Builder {
            params.mContentView = view
            params.mContentViewLayoutResId = 0
            return this
        }

        public fun build(): WearDialogParams {
            return params
        }
    }
}