package com.thewizrd.simpleweather.widgets.preferences

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.XmlRes
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.thewizrd.common.preferences.SliderPreference
import com.thewizrd.common.utils.glide.TransparentOverlay
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.preferences.colorpreference.ColorPreference
import com.thewizrd.simpleweather.widgets.AppChoiceDialogBuilder
import com.thewizrd.simpleweather.widgets.WidgetUtils

abstract class BaseWeatherWidgetPreferenceFragment : AbstractWeatherWidgetPreferenceFragment() {
    protected lateinit var hideSettingsBtnPref: SwitchPreference
    protected lateinit var hideRefreshBtnPref: SwitchPreference

    protected lateinit var bgChoicePref: ListPreference
    protected lateinit var bgColorPref: ColorPreference
    protected lateinit var txtColorPref: ColorPreference
    protected lateinit var bgStylePref: ListPreference

    protected lateinit var textSizePref: SliderPreference
    protected lateinit var iconSizePref: SliderPreference

    protected lateinit var clockPref: Preference
    protected lateinit var calPref: Preference

    @XmlRes
    abstract fun getPreferencesResId(): Int

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(getPreferencesResId(), rootKey)

        hideSettingsBtnPref = findPreference(KEY_HIDESETTINGSBTN)!!
        hideRefreshBtnPref = findPreference(KEY_HIDEREFRESHBTN)!!

        hideSettingsBtnPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putBoolean(KEY_HIDESETTINGSBTN, newValue as Boolean)
                hideSettingsBtnPref.isChecked = newValue
                updateWidgetView()
                true
            }

        hideRefreshBtnPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putBoolean(KEY_HIDEREFRESHBTN, newValue as Boolean)
                hideRefreshBtnPref.isChecked = newValue
                updateWidgetView()
                true
            }

        hideSettingsBtnPref.isChecked = WidgetUtils.isSettingsButtonHidden(mAppWidgetId)

        if (!WidgetUtils.isSettingsButtonOptional(mWidgetType)) {
            hideSettingsBtnPref.isVisible = false
        }

        hideRefreshBtnPref.isChecked = WidgetUtils.isRefreshButtonHidden(mAppWidgetId)

        if (WidgetUtils.isMaterialYouWidget(mWidgetType)) {
            hideRefreshBtnPref.isVisible = false
        }

        // Time and Date
        clockPref = findPreference(KEY_CLOCKAPP)!!
        calPref = findPreference(KEY_CALENDARAPP)!!

        clockPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let {
                AppChoiceDialogBuilder(it)
                    .setOnItemSelectedListener(object :
                        AppChoiceDialogBuilder.OnAppSelectedListener {
                        override fun onItemSelected(key: String?) {
                            WidgetUtils.setOnClickClockApp(key)
                            updateClockPreference(it)
                        }
                    }).show()
            }
            true
        }
        calPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let {
                AppChoiceDialogBuilder(it)
                    .setOnItemSelectedListener(object :
                        AppChoiceDialogBuilder.OnAppSelectedListener {
                        override fun onItemSelected(key: String?) {
                            WidgetUtils.setOnClickCalendarApp(key)
                            updateCalPreference(it)
                        }
                    }).show()
            }
            true
        }

        if (WidgetUtils.isClockWidget(mWidgetType)) {
            updateClockPreference(requireContext())
            clockPref.isVisible = true
        } else {
            clockPref.isVisible = false
        }

        if (WidgetUtils.isDateWidget(mWidgetType)) {
            updateCalPreference(requireContext())
            calPref.isVisible = true
        } else {
            calPref.isVisible = false
        }

        findPreference<Preference>(KEY_CATCLOCKDATE)!!.isVisible =
            WidgetUtils.isClockWidget(mWidgetType) || WidgetUtils.isDateWidget(mWidgetType)

        textSizePref = findPreference(KEY_TEXTSIZE)!!
        textSizePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putFloat(KEY_TEXTSIZE, newValue as Float)
                updateWidgetView()

                true
            }

        iconSizePref = findPreference(KEY_ICONSIZE)!!
        iconSizePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putFloat(KEY_ICONSIZE, newValue as Float)
                updateWidgetView()
                true
            }

        if (WidgetUtils.isCustomSizeWidget(mWidgetType)) {
            textSizePref.setValue(WidgetUtils.getCustomTextSizeMultiplier(mAppWidgetId))
            textSizePref.callChangeListener(textSizePref.getValue())
            iconSizePref.setValue(WidgetUtils.getCustomIconSizeMultiplier(mAppWidgetId))
            iconSizePref.callChangeListener(iconSizePref.getValue())

            findPreference<Preference>(KEY_CATCUSTOMSIZE)?.isVisible = true
        } else {
            findPreference<Preference>(KEY_CATCUSTOMSIZE)?.isVisible = false
        }

        // Widget background style
        bgChoicePref = findPreference(KEY_BGCOLOR)!!
        bgStylePref = findPreference(KEY_BGSTYLE)!!
        bgColorPref = findPreference(KEY_BGCOLORCODE)!!
        txtColorPref = findPreference(KEY_TXTCOLORCODE)!!

        bgChoicePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val value = newValue.toString().toInt()

                val mWidgetBackground = WidgetUtils.WidgetBackground.valueOf(value)
                mWidgetOptions.putSerializable(KEY_BGCOLOR, mWidgetBackground)

                updateWidgetView()

                bgColorPref.isVisible = mWidgetBackground == WidgetUtils.WidgetBackground.CUSTOM
                txtColorPref.isVisible = mWidgetBackground == WidgetUtils.WidgetBackground.CUSTOM

                if (mWidgetBackground == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
                    if (WidgetUtils.isPandaWidget(mWidgetType)) {
                        bgStylePref.isVisible = true
                        return@OnPreferenceChangeListener true
                    }
                }

                bgStylePref.setValueIndex(0)
                bgStylePref.callChangeListener(bgStylePref.value)
                bgStylePref.isVisible = false
                true
            }

        bgStylePref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putSerializable(
                    KEY_BGSTYLE,
                    WidgetUtils.WidgetBackgroundStyle.valueOf(newValue.toString().toInt())
                )
                updateWidgetView()
                true
            }

        bgColorPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putInt(KEY_BGCOLORCODE, newValue as Int)
                updateWidgetView()
                true
            }

        txtColorPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mWidgetOptions.putInt(KEY_TXTCOLORCODE, newValue as Int)
                updateWidgetView()
                true
            }

        val styles = WidgetUtils.WidgetBackgroundStyle.values()
        val styleEntries = arrayOfNulls<CharSequence>(styles.size)
        val styleEntryValues = arrayOfNulls<CharSequence>(styles.size)

        for (i in styles.indices) {
            when (val style = styles[i]) {
                WidgetUtils.WidgetBackgroundStyle.PANDA -> {
                    styleEntries[i] = requireContext().getString(R.string.label_style_panda)
                    styleEntryValues[i] = style.value.toString()
                    bgStylePref.setDefaultValue(styleEntryValues[i])
                }
                WidgetUtils.WidgetBackgroundStyle.DARK -> {
                    styleEntries[i] = requireContext().getText(R.string.label_style_dark)
                    styleEntryValues[i] = style.value.toString()
                }
                WidgetUtils.WidgetBackgroundStyle.LIGHT -> {
                    styleEntries[i] = requireContext().getText(R.string.label_style_light)
                    styleEntryValues[i] = style.value.toString()
                }
            }
        }
        bgStylePref.entries = styleEntries
        bgStylePref.entryValues = styleEntryValues

        val mWidgetBackground = WidgetUtils.getWidgetBackground(mAppWidgetId)
        val mWidgetBGStyle = WidgetUtils.getBackgroundStyle(mAppWidgetId)
        @ColorInt val mWidgetBackgroundColor = WidgetUtils.getBackgroundColor(mAppWidgetId)
        @ColorInt val mWidgetTextColor = WidgetUtils.getTextColor(mAppWidgetId)

        if (WidgetUtils.isBackgroundOptionalWidget(mWidgetType)) {
            bgChoicePref.setValueIndex(mWidgetBackground.value)
            bgChoicePref.callChangeListener(bgChoicePref.value)

            bgStylePref.setValueIndex(
                listOf(*WidgetUtils.WidgetBackgroundStyle.values()).indexOf(
                    mWidgetBGStyle
                )
            )
            bgStylePref.callChangeListener(bgStylePref.value)

            bgColorPref.color = mWidgetBackgroundColor
            bgColorPref.callChangeListener(bgColorPref.color)
            txtColorPref.color = mWidgetTextColor
            txtColorPref.callChangeListener(txtColorPref.color)

            findPreference<Preference>(KEY_BACKGROUND)!!.isVisible = true
            if (WidgetUtils.isBackgroundCustomOnlyWidget(mWidgetType)) {
                bgChoicePref.isVisible = false
                bgStylePref.isVisible = false
            }
        } else {
            bgChoicePref.setValueIndex(WidgetUtils.WidgetBackground.TRANSPARENT.value)
            findPreference<Preference>(KEY_BACKGROUND)!!.isVisible = false
        }
    }

    private fun updateClockPreference(context: Context) {
        val componentName = WidgetUtils.getClockAppComponent(context)
        if (componentName != null) {
            try {
                val appInfo =
                    context.packageManager.getApplicationInfo(componentName.packageName, 0)
                val appLabel = context.packageManager.getApplicationLabel(appInfo)
                clockPref.summary = appLabel
                return
            } catch (e: PackageManager.NameNotFoundException) {
                // App not available
                WidgetUtils.setOnClickClockApp(null)
            }
        }

        clockPref.setSummary(R.string.summary_default)
    }

    private fun updateCalPreference(context: Context) {
        val componentName = WidgetUtils.getCalendarAppComponent(context)
        if (componentName != null) {
            try {
                val appInfo =
                    context.packageManager.getApplicationInfo(componentName.packageName, 0)
                val appLabel = context.packageManager.getApplicationLabel(appInfo)
                calPref.summary = appLabel
                return
            } catch (e: PackageManager.NameNotFoundException) {
                // App not available
                WidgetUtils.setOnClickClockApp(null)
            }
        }

        calPref.setSummary(R.string.summary_default)
    }

    protected fun updateBackground() {
        binding.widgetContainer.findViewById<View>(R.id.widget)?.run {
            if (background == null) {
                setBackgroundResource(R.drawable.app_widget_background_mask)
                clipToOutline = true
            }
        }

        if (WidgetUtils.WidgetBackground.valueOf(bgChoicePref.value.toInt()) == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
            val imageView = binding.widgetContainer.findViewById<ImageView>(R.id.widgetBackground)
            if (imageView != null) {
                mGlide.load("file:///android_asset/backgrounds/day.jpg")
                    .apply(
                        RequestOptions.noTransformation()
                            .format(DecodeFormat.PREFER_RGB_565)
                            .transform(
                                TransparentOverlay(0x33),
                                CenterCrop()
                            )
                    )
                    .thumbnail(0.75f)
                    .into(imageView)
            }
        }
    }

    @CallSuper
    override fun finalizeWidgetUpdate() {
        // Save widget preferences
        WidgetUtils.setWidgetBackground(mAppWidgetId, bgChoicePref.value.toInt())
        WidgetUtils.setBackgroundColor(mAppWidgetId, bgColorPref.color)
        WidgetUtils.setTextColor(mAppWidgetId, txtColorPref.color)
        WidgetUtils.setBackgroundStyle(mAppWidgetId, bgStylePref.value.toInt())
        WidgetUtils.setSettingsButtonHidden(mAppWidgetId, hideSettingsBtnPref.isChecked)
        WidgetUtils.setRefreshButtonHidden(mAppWidgetId, hideRefreshBtnPref.isChecked)

        if (WidgetUtils.isCustomSizeWidget(mWidgetType)) {
            WidgetUtils.setCustomTextSizeMultiplier(mAppWidgetId, textSizePref.getValue())
            WidgetUtils.setCustomIconSizeMultiplier(mAppWidgetId, iconSizePref.getValue())
        }

        pushWidgetUpdate()
    }
}