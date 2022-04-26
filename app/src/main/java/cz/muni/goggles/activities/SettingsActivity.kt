package cz.muni.goggles.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cz.muni.goggles.R
import cz.muni.goggles.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, SettingsFragment()).commit()
    }
}
