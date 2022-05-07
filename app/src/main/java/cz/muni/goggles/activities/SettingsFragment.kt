package cz.muni.goggles.activities

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import cz.muni.goggles.R


class SettingsFragment : PreferenceFragmentCompat()
{

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
    {
        addPreferencesFromResource(R.xml.preferences)
    }

}
