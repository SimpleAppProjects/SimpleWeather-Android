package com.thewizrd.simpleweather.preferences

import android.content.ClipData
import android.content.ClipboardManager
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
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.extras.updateFirebaseIdPreference
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class DevSettingsFragment : ToolbarPreferenceFragmentCompat() {

    override val titleResId: Int
        get() = sharedRes.string.title_dev_settings

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
        val firebaseCategory: PreferenceCategory
        val miscCategory: PreferenceCategory

        preferenceScreen.addPreference(PreferenceCategory(context).apply {
            title = "API Keys"
        }.also { apiKeyCategory = it })
        preferenceScreen.addPreference(PreferenceCategory(context).apply {
            title = "Firebase"
            isVisible = !BuildConfig.IS_NONGMS
        }.also { firebaseCategory = it })
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
                    settingsManager.setPersonalKey(preference.key, true)
                    true
                }
            isIconSpaceReserved = false
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
                    settingsManager.setPersonalKey(preference.key, true)
                    true
                }
            isIconSpaceReserved = false
        })

        apiKeyCategory.addPreference(EditTextPreference(context).apply {
            title = "Google Weather Key"
            dialogTitle = "API Key"

            key = WeatherAPI.GOOGLE
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                settingsManager.getAPIKey(WeatherAPI.GOOGLE) ?: "null"
            }
            isPersistent = false
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    settingsManager.setAPIKey(preference.key, newValue?.toString())
                    settingsManager.setPersonalKey(preference.key, true)
                    true
                }
            isIconSpaceReserved = false
        })

        apiKeyCategory.addPreference(EditTextPreference(context).apply {
            title = "Google Pollen Key"
            dialogTitle = "API Key"

            key = WeatherAPI.GOOGLE_POLLEN
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                settingsManager.getAPIKey(WeatherAPI.GOOGLE_POLLEN) ?: "null"
            }
            isPersistent = false
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    settingsManager.setAPIKey(preference.key, newValue?.toString())
                    settingsManager.setPersonalKey(preference.key, true)
                    true
                }
            isIconSpaceReserved = false
        })

        firebaseCategory.addPreference(Preference(context).apply {
            title = "Firebase Id"
            isPersistent = false
            onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference ->
                    val clipService =
                        preference.context.getSystemService(ClipboardManager::class.java)
                    val clipData = ClipData.newPlainText("Firebase Id", preference.summary)
                    clipService.setPrimaryClip(clipData)

                    Toast.makeText(preference.context, "Copied to clipboard", Toast.LENGTH_SHORT)
                        .show()
                    true
                }
            isIconSpaceReserved = false
        }.also { preference ->
            updateFirebaseIdPreference(preference)
        })

        miscCategory.addPreference(SwitchPreference(context).apply {
            title = "Enable Debug Mode"
            isPersistent = false
            isChecked = Logger.isDebugLoggerEnabled()
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    Logger.enableDebugLogger(context, newValue as Boolean)
                    true
                }
            isIconSpaceReserved = false
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

                    logFiles.sortByDescending { it.name }

                    val logFileArr = logFiles
                        .map { it.name }
                        .toTypedArray()

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
            isIconSpaceReserved = false
        })
    }
}