package com.github.zly2006.zhihu.ui

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.catching

class SettingsFragment : PreferenceFragmentCompat() {
    private var clickCounter = 0
    private val developer = MutableLiveData(false)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val developerCategory: PreferenceCategory? = findPreference("developers")
        val developerPreference: SwitchPreferenceCompat? = findPreference("developer")
        developerPreference?.setOnPreferenceChangeListener { _, newValue ->
            developer.value = newValue as Boolean
            true
        }
        developerCategory?.addPreference(
            Preference(requireContext()).apply {
                key = "crash_test"
                title = "Crash Test"
                setOnPreferenceClickListener {
                    activity?.catching {
                        throw RuntimeException("Crash Test")
                    }
                    return@setOnPreferenceClickListener true
                }
            }
        )
        developer.observe(this) {
            developerCategory?.isVisible = it
        }
        developer.value = developerPreference?.isChecked

        val versionPreference: Preference? = findPreference("version")
        versionPreference?.summary = "${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}"
        versionPreference?.setOnPreferenceClickListener {
            clickCounter++
            if (clickCounter == 3) {
                clickCounter = Int.MIN_VALUE
                developerPreference?.isChecked = true
                developer.value = true
                Toast.makeText(context, "You are now a developer", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }
}
