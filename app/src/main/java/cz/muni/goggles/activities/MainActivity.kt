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
import android.widget.CheckBox
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.android.material.slider.RangeSlider
import cz.muni.goggles.*
import cz.muni.goggles.R
import cz.muni.goggles.adapter.Adapter
import cz.muni.goggles.classes.Game
import cz.muni.goggles.classes.Products
import cz.muni.goggles.database.SGame
import cz.muni.goggles.database.SGameDao
import cz.muni.goggles.database.SGameDatabase
import cz.muni.goggles.database.SGameRepository
import cz.muni.goggles.worker.PriceCheckWorker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://catalog.gog.com/v1/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val retrofitGog: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.gog.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val service: Api = retrofit.create(Api::class.java)
    private val gogService: GogApi = retrofitGog.create(GogApi::class.java)
    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private var query: String = ""
    private lateinit var priceTextView: TextView
    private lateinit var priceRangeSlider: RangeSlider
    private lateinit var adapter: Adapter
    private lateinit var context: Context
    private var upcoming = false
    private var following = false

    private val channelId = "channelID"
    private val channelName = "Subscription"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()

        context = applicationContext
        val recyclerView = findViewById<RecyclerView>(R.id.recycler)
        recyclerView.layoutManager = GridLayoutManager(this@MainActivity, 2)
        adapter = Adapter()
        recyclerView.adapter = adapter


        priceRangeSlider = findViewById(R.id.priceRangeSlider)
        priceTextView = findViewById(R.id.priceTextView)

        priceRangeSlider.addOnChangeListener { _, _, _ ->
            updatePrice()
        }

        priceRangeSlider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: RangeSlider) { }

            override fun onStopTrackingTouch(slider: RangeSlider) {
                refresh()
            }
        })

        val checkboxUpcoming = findViewById<CheckBox>(R.id.checkBoxUpcoming)
        checkboxUpcoming.setOnCheckedChangeListener { _, checked ->
            upcoming = checked
            refresh()
        }

        val checkboxFollowing = findViewById<CheckBox>(R.id.checkBoxFollowing)
        checkboxFollowing.setOnCheckedChangeListener { _, checked ->
            following = checked
            getFollowing()
        }

        val priceCheckWorkRequest: WorkRequest =
            PeriodicWorkRequestBuilder<PriceCheckWorker>(4,TimeUnit.HOURS)
                .addTag("PRICE_CHECK")
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

        WorkManager
            .getInstance(this)
            .enqueue(priceCheckWorkRequest)

    }

    override fun onResume() {
        super.onResume()
        updatePrice()
        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_scrolling, menu)

        val search = menu?.findItem(R.id.nav_search)
        val searchView = search?.actionView as SearchView

        searchView.setOnCloseListener {
            refresh()
            false
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                refresh()
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                query = p0 ?: ""
                return false
            }

        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refresh() {
            service.getSearchByName(
                sharedPreferences.getString("currency", "EUR") ?: "EUR",
                if (query.isBlank()) null else "like:$query",
                "between%3A${priceRangeSlider.values[0]}%2C${priceRangeSlider.values[1]}",
                if (upcoming) "in:upcoming" else null
            ).enqueue(object : Callback<Products> {
                override fun onResponse(call: Call<Products>, response: Response<Products>) {
                    val responseBody = response.body()
                    if (response.isSuccessful && responseBody != null) {
                        println(responseBody.products)
                        adapter.setItems(responseBody.products)
                    }
                }

                override fun onFailure(call: Call<Products>, t: Throwable) {
                    println(t.message)
                    Toast.makeText(this@MainActivity, "${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

    }

    private fun getFollowing() {
        val games = SGameDatabase.getDatabase(context).sGameDao().getAll()
        val gamesList:MutableList<Game> = mutableListOf<Game>()
        for (game in games) {
            gogService.getProductsByIds(
                game.productId.toString()
            ).enqueue(object : Callback<Game> {
                override fun onResponse(call: Call<Game>, response: Response<Game>) {
                    print(call.request().toString())
                    val responseBody = response.body()
                    println("SPM TU")
                    println(responseBody)
                    if (response.isSuccessful && responseBody != null) {
                        println(responseBody)
                        gamesList.add(responseBody)
                        adapter.setItems(gamesList)
                    }

                }

                override fun onFailure(call: Call<Game>, t: Throwable) {
                    println("SPM TU FAIL")
                    println(t.message)
                    Toast.makeText(this@MainActivity, "${t.message}", Toast.LENGTH_SHORT).show()
                }


            })

        }

    }

    private fun updatePrice() {
        val numberFormat = NumberFormat.getCurrencyInstance()
        numberFormat.currency = Currency.getInstance(sharedPreferences.getString("currency", "EUR"))

        priceTextView.text = getString(
            R.string.price,
            numberFormat.format(priceRangeSlider.values[0]),
            numberFormat.format(priceRangeSlider.values[1])
        )
    }

    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
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
