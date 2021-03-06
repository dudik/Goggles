package cz.muni.goggles.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.work.*
import cz.muni.goggles.R
import cz.muni.goggles.databinding.ActivitySettingsBinding
import cz.muni.goggles.worker.PriceCheckWorker
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity()
{
    private lateinit var binding: ActivitySettingsBinding
    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val preferencesListener = onSharedPreferenceChangeListener()
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesListener)

        // showing the back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, SettingsFragment()).commit()

    }

    override fun onSupportNavigateUp(): Boolean
    {
        finish()
        return true
    }

    private fun onSharedPreferenceChangeListener(): SharedPreferences.OnSharedPreferenceChangeListener
    {
        val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key.equals("repeatInterval"))
            {
                val priceCheckWorkRequest: PeriodicWorkRequest = PeriodicWorkRequestBuilder<PriceCheckWorker>(
                    sharedPreferences.getString("repeatInterval", "4")!!.toLong(), TimeUnit.HOURS).addTag("PRICE_CHECK")
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setInitialDelay(sharedPreferences.getString("repeatInterval", "4")!!.toLong(), TimeUnit.HOURS)
                    .build()

                WorkManager.getInstance(this).enqueueUniquePeriodicWork("priceCheck", ExistingPeriodicWorkPolicy.REPLACE, priceCheckWorkRequest)
            }
        }
        return preferencesListener
    }
}
