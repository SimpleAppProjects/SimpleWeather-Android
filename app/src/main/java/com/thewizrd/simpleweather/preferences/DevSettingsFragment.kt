package com.thewizrd.simpleweather.preferences

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.R
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class DevSettingsFragment : ToolbarPreferenceFragmentCompat() {

    override val titleResId: Int
        get() = R.string.title_dev_settings

    private lateinit var intentLauncher: ActivityResultLauncher<String>
    private var mLogFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intentLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("text/*")) { uri ->
                if (uri != null) {
                    mLogFile?.let { file ->
                        runCatching {
                            val docFile = DocumentFile.fromSingleUri(requireContext(), uri)!!
                            requireContext().contentResolver.openFileDescriptor(docFile.uri, "w")!!
                                .use { pfd ->
                                    val fs = FileOutputStream(pfd.fileDescriptor)
                                    file.inputStream().use {
                                        it.copyTo(fs)
                                    }
                                }
                            Toast.makeText(requireContext(), "File saved", Toast.LENGTH_SHORT)
                                .show()
                        }.onFailure {
                            Timber.tag(DevSettingsFragment::class.java.name)
                                .e(it, "Error saving log")
                            context?.let { ctx ->
                                Toast.makeText(ctx, "Error saving log!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()

        preferenceScreen = preferenceManager.createPreferenceScreen(context)

        val apiKeyCategory: PreferenceCategory
        val miscCategory: PreferenceCategory

        preferenceScreen.addPreference(PreferenceCategory(context).apply {
            title = "API Keys"
        }.also { apiKeyCategory = it })
        preferenceScreen.addPreference(PreferenceCategory(context).apply {
            title = "Misc"
        }.also { miscCategory = it })

        apiKeyCategory.addPreference(EditTextPreference(context).apply {
            title = "AccuWeather Key"
            dialogTitle = "API Key"

            key = WeatherAPI.ACCUWEATHER
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                settingsManager.getAPIKey(WeatherAPI.ACCUWEATHER) ?: "null"
            }
            isPersistent = false
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    settingsManager.setAPIKey(preference.key, newValue?.toString())
                    true
                }
        })

        apiKeyCategory.addPreference(EditTextPreference(context).apply {
            title = "Tomorrow.io Key"
            dialogTitle = "API Key"

            key = WeatherAPI.TOMORROWIO
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                settingsManager.getAPIKey(WeatherAPI.TOMORROWIO) ?: "null"
            }
            isPersistent = false
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    settingsManager.setAPIKey(preference.key, newValue?.toString())
                    true
                }
        })

        miscCategory.addPreference(SwitchPreference(context).apply {
            title = "Enable Debug Logger"
            isPersistent = false
            isChecked = Logger.isDebugLoggerEnabled()
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    Logger.enableDebugLogger(context, newValue as Boolean)
                    true
                }
        })

        miscCategory.addPreference(Preference(context).apply {
            title = "Save Logs"
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                runCatching {
                    val logsDirectory = File(context.getExternalFilesDir(null).toString() + "/logs")
                    val logFiles = logsDirectory.listFiles()

                    if (logFiles.isNullOrEmpty()) {
                        Toast.makeText(it.context, "No logs found", Toast.LENGTH_SHORT).show()
                        return@runCatching
                    }

                    val logFileArr = logFiles.map { it.name }.toTypedArray()

                    MaterialAlertDialogBuilder(it.context)
                        .setItems(logFileArr) { dialog, which ->
                            logFiles[which]?.let { file ->
                                intentLauncher.launch(file.name)
                                mLogFile = file
                            }
                            dialog.dismiss()
                        }
                        .setTitle(it.title)
                        .setPositiveButton(null, null)
                        .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                            dialog.cancel()
                        }
                        .show()
                }
                true
            }
        })
    }
}