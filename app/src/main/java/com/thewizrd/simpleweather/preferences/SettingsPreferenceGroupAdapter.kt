/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thewizrd.simpleweather.preferences

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.core.graphics.ColorUtils
import androidx.core.view.updatePaddingRelative
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceViewHolder
import com.google.android.material.R as materialRes
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R

/**
 * A custom adapter for displaying settings preferences in a list, handling rounded corners for
 * preference items within a group.
 * Based on AOSP SettingsPreferenceGroupAdapter
 */
@SuppressLint("RestrictedApi")
class SettingsPreferenceGroupAdapter(preferenceGroup: PreferenceGroup) :
    PreferenceGroupAdapter(preferenceGroup) {

    private val mPreferenceGroup = preferenceGroup
    private var mRoundCornerMappingList: ArrayList<Int> = ArrayList()

    private var mNormalPaddingStart = 0
    private var mGroupPaddingStart = 0
    private var mNormalPaddingEnd = 0
    private var mGroupPaddingEnd = 0

    @DrawableRes
    private var mLegacyBackgroundRes: Int

    private val mHandler = Handler(Looper.getMainLooper())

    private val syncRunnable = Runnable { updatePreferencesList() }

    init {
        val context = preferenceGroup.context
        mNormalPaddingStart =
            context.resources.getDimensionPixelSize(R.dimen.preference_expressive_space_small1)
        mGroupPaddingStart = mNormalPaddingStart * 2
        mNormalPaddingEnd =
            context.resources.getDimensionPixelSize(R.dimen.preference_expressive_space_small1)
        mGroupPaddingEnd = mNormalPaddingEnd * 2
        val outValue = TypedValue()
        context.theme.resolveAttribute(
            android.R.attr.selectableItemBackground,
            outValue,
            true, /* resolveRefs */
        )
        mLegacyBackgroundRes = outValue.resourceId
        updatePreferencesList()
    }

    @SuppressLint("RestrictedApi")
    override fun onPreferenceHierarchyChange(preference: Preference) {
        super.onPreferenceHierarchyChange(preference)
        // Post after super class has posted their sync runnable to update preferences.
        mHandler.removeCallbacks(syncRunnable)
        mHandler.post(syncRunnable)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.isDividerAllowedAbove = false
        holder.isDividerAllowedBelow = false
        updateBackground(holder, position)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updatePreferencesList() {
        val oldList = ArrayList(mRoundCornerMappingList)
        mRoundCornerMappingList = ArrayList()
        mappingPreferenceGroup(mRoundCornerMappingList, mPreferenceGroup)
        if (mRoundCornerMappingList != oldList) {
            notifyDataSetChanged()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun mappingPreferenceGroup(cornerStyles: MutableList<Int>, group: PreferenceGroup) {
        cornerStyles.clear()
        cornerStyles.addAll(MutableList(itemCount) { 0 })

        // the first item in to group
        var startIndex = -1
        // the last item in the group
        var endIndex = -1
        var currentParent: PreferenceGroup? = group
        for (i in 0 until itemCount) {
            when (val pref = getItem(i)) {
                // do not apply round corner background
                is NoBackground,
                is Selectable -> {
                    cornerStyles[i] = 0
                    startIndex = -1
                    endIndex = -1
                }

                // PreferenceCategory should not have round corner background.
                is PreferenceCategory -> {
                    cornerStyles[i] = 0
                    startIndex = -1
                    endIndex = -1
                    currentParent = pref
                }

                else -> {
                    val parent = pref?.parent

                    // item in the group should have round corner background.
                    cornerStyles[i] = cornerStyles[i] or ROUND_CORNER_CENTER
                    if (parent === currentParent) {
                        // find the first item in the group
                        if (startIndex == -1) {
                            startIndex = i
                            cornerStyles[i] = cornerStyles[i] or ROUND_CORNER_TOP
                        }

                        // find the last item in the group, if we find the new last item, we should
                        // remove the old last item round corner.
                        if (endIndex == -1 || endIndex < i) {
                            if (endIndex != -1) {
                                cornerStyles[endIndex] =
                                    cornerStyles[endIndex] and ROUND_CORNER_BOTTOM.inv()
                            }
                            endIndex = i
                            cornerStyles[i] = cornerStyles[i] or ROUND_CORNER_BOTTOM
                        }
                    } else {
                        // this item is new group, we should reset the index.
                        currentParent = parent
                        startIndex = i
                        cornerStyles[i] = cornerStyles[i] or ROUND_CORNER_TOP
                        endIndex = i
                        cornerStyles[i] = cornerStyles[i] or ROUND_CORNER_BOTTOM
                    }
                }
            }
        }
    }

    /** handle roundCorner background */
    private fun updateBackground(holder: PreferenceViewHolder, position: Int) {
        @DrawableRes
        val backgroundRes = if (getItem(position) is Selectable) {
            mLegacyBackgroundRes
        } else {
            getRoundCornerDrawableRes(position)
        }

        val v = holder.itemView
        // Update padding
        val (paddingStart, paddingEnd) = getStartEndPadding(position, backgroundRes)
        v.updatePaddingRelative(start = paddingStart, top = 0, bottom = 0, end = paddingEnd)
        v.clipToOutline = backgroundRes != 0 && backgroundRes != mLegacyBackgroundRes

        // Update background
        v.setBackgroundResource(backgroundRes)

        if (settingsManager.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            v.backgroundTintList = ColorStateList.valueOf(
                ColorUtils.compositeColors(
                    v.context.getAttrColor(materialRes.attr.colorSurfaceDim),
                    Colors.BLACK
                )
            )
        } else {
            v.backgroundTintList = null
        }
    }

    private fun getStartEndPadding(position: Int, backgroundRes: Int): Pair<Int, Int> {
        val item = getItem(position)
        return when {
            // According to mappingPreferenceGroup(), backgroundRes == 0 means this item is
            // GroupSectionDividerMixin or PreferenceCategory, which is design to have normal
            // padding.
            // NormalPaddingMixin items are also designed to have normal padding.
            backgroundRes == 0 || backgroundRes == mLegacyBackgroundRes ->
                mNormalPaddingStart to mNormalPaddingEnd

            // Other items are suppose to have group padding.
            else -> mGroupPaddingStart to mGroupPaddingEnd
        }
    }

    @DrawableRes
    protected fun getRoundCornerDrawableRes(position: Int): Int {
        val cornerType = mRoundCornerMappingList[position]

        if ((cornerType and ROUND_CORNER_CENTER) == 0) {
            return 0
        }

        return when {
            (cornerType and ROUND_CORNER_TOP) != 0 && (cornerType and ROUND_CORNER_BOTTOM) == 0 -> {
                // the first
                R.drawable.preference_round_background_top
            }

            (cornerType and ROUND_CORNER_BOTTOM) != 0 && (cornerType and ROUND_CORNER_TOP) == 0 -> {
                // the last
                R.drawable.preference_round_background_bottom
            }

            (cornerType and ROUND_CORNER_TOP) != 0 && (cornerType and ROUND_CORNER_BOTTOM) != 0 -> {
                // the only one preference
                R.drawable.preference_round_background
            }

            else -> {
                // in the center
                R.drawable.preference_round_background_center
            }
        }
    }

    companion object {
        private const val ROUND_CORNER_CENTER: Int = 1
        private const val ROUND_CORNER_TOP: Int = 1 shl 1
        private const val ROUND_CORNER_BOTTOM: Int = 1 shl 2
    }
}