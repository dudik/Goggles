package cz.muni.goggles.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.android.material.slider.RangeSlider
import cz.muni.goggles.R
import cz.muni.goggles.adapter.Adapter
import cz.muni.goggles.databinding.ActivityMainBinding
import cz.muni.goggles.logic.getFollowing
import cz.muni.goggles.logic.refresh
import cz.muni.goggles.worker.PriceCheckWorker
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: Adapter

    private var query: String = ""
    private var upcoming = false
    private var following = false
    private var isStartedFromNotification = false
    private var pageNumber = 1

    private val channelId = "goggles"
    private val channelName = "Price"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        createNotificationChannel()

        setActivityIfStartedFromNotification()
        setRecycler()
        setPriceSlider()
        setCheckBoxes()
        setWorkerForPriceCheck()
    }

    override fun onResume()
    {
        super.onResume()
        updatePrice()
        if (following)
        {
            getFollowing(this, adapter)
        }
        else
        {
            refresh(this, binding, sharedPreferences, adapter, query, upcoming)
        }
    }

    private fun setCheckBoxes()
    {
        binding.checkBoxUpcoming.setOnCheckedChangeListener { _, checked ->
            pageNumber = 1
            upcoming = checked
            refresh(this, binding, sharedPreferences, adapter, query, upcoming)
        }

        binding.checkBoxFollowing.setOnCheckedChangeListener { _, checked ->
            pageNumber = 1
            following = checked
            if (following)
            {
                getFollowing(this, adapter)
            }
            else
            {
                refresh(this, binding, sharedPreferences, adapter, query, upcoming)
            }
        }
    }

    private fun setPriceSlider() {
        binding.priceRangeSlider.addOnChangeListener { _, _, _ ->
            pageNumber = 1
            updatePrice()
        }

        binding.priceRangeSlider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: RangeSlider) {}

            override fun onStopTrackingTouch(slider: RangeSlider) {
                refresh(this@MainActivity, binding, sharedPreferences, adapter, query, upcoming)
            }
        })
    }

    private fun setRecycler() {
        binding.recycler.layoutManager = GridLayoutManager(this@MainActivity, 2)
        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1) && dy != 0 && (!following && !upcoming)) {
                    pageNumber++
                    refresh(this@MainActivity, binding, sharedPreferences, adapter, query, upcoming, pageNumber)
                }
            }
        })

        adapter = Adapter()
        binding.recycler.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_scrolling, menu)

        val search = menu?.findItem(R.id.nav_search)
        val searchView = search?.actionView as SearchView

        searchView.setOnCloseListener {
            refresh(this, binding, sharedPreferences, adapter, query, upcoming)
            false
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(searchedString: String?): Boolean {
                refresh(this@MainActivity, binding, sharedPreferences, adapter, query, upcoming)
                return false
            }

            override fun onQueryTextChange(searchedString: String?): Boolean {
                query = searchedString ?: ""
                refresh(this@MainActivity, binding, sharedPreferences, adapter, query, upcoming)
                return false
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        when (item.itemId)
        {
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setActivityIfStartedFromNotification()
    {
        val extras = intent.extras

        if (extras != null)
        {
            isStartedFromNotification = extras.getBoolean("fromNotification")
        }
        if (isStartedFromNotification)
        {
            binding.checkBoxFollowing.isChecked = true
            following = true
        }
    }

    private fun setWorkerForPriceCheck()
    {
        val priceCheckWorkRequest: WorkRequest = PeriodicWorkRequestBuilder<PriceCheckWorker>(
            sharedPreferences.getString("repeatInterval", "4")!!.toLong(), TimeUnit.HOURS).addTag("PRICE_CHECK")
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()

        WorkManager.getInstance(this).enqueue(priceCheckWorkRequest)
    }

    private fun updatePrice()
    {
        val numberFormat = NumberFormat.getCurrencyInstance()
        numberFormat.currency = Currency.getInstance(sharedPreferences.getString("currency", "EUR"))

        binding.priceTextView.text = getString(
            R.string.price, numberFormat.format(binding.priceRangeSlider.values[0]), numberFormat.format(binding.priceRangeSlider.values[1]))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply {
                        lightColor = Color.BLUE
                        enableLights(true)
                        enableVibration(true)
                    }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}