package cz.muni.goggles.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cz.muni.goggles.R
import cz.muni.goggles.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity()
{

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // showing the back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, SettingsFragment()).commit()

    }

    // this event will enable the back
    // function to the button on press
    override fun onSupportNavigateUp(): Boolean
    {
        finish()
        return true
    }
}
