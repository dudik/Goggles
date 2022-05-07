package cz.muni.goggles.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.android.material.slider.RangeSlider
import cz.muni.goggles.R
import cz.muni.goggles.SGameApplication
import cz.muni.goggles.adapter.Adapter
import cz.muni.goggles.api.CatalogApi
import cz.muni.goggles.api.ProductIdApi
import cz.muni.goggles.classes.Game
import cz.muni.goggles.classes.Price
import cz.muni.goggles.classes.Products
import cz.muni.goggles.database.SGameViewModel
import cz.muni.goggles.database.SGameViewModelFactory
import cz.muni.goggles.worker.PriceCheckWorker
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
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

    private val service: CatalogApi = retrofit.create(CatalogApi::class.java)
    private val productIdService: ProductIdApi = retrofitGog.create(ProductIdApi::class.java)
    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private var query: String = ""
    private lateinit var priceTextView: TextView
    private lateinit var priceRangeSlider: RangeSlider
    private lateinit var adapter: Adapter
    private lateinit var context: Context
    private var upcoming = false
    private var following = false
    private var isStartedFromNotification = false
    private var pageNumber = 1

    private val channelId = "channelID"
    private val channelName = "Subscription"

    val tag = "MainActivityLog"

    private val sGameViewModel: SGameViewModel by viewModels {
        SGameViewModelFactory((application as SGameApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()
        val checkboxUpcoming = findViewById<CheckBox>(R.id.checkBoxUpcoming)
        val checkboxFollowing = findViewById<CheckBox>(R.id.checkBoxFollowing)

        val extras = intent.extras
        if (extras != null){
            isStartedFromNotification = extras.getBoolean("fromNotification")
        }
        if (isStartedFromNotification){
            checkboxFollowing.isChecked = true
            following = true
        }

        context = applicationContext
        val recyclerView = findViewById<RecyclerView>(R.id.recycler)
        recyclerView.layoutManager = GridLayoutManager(this@MainActivity, 2)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1) && dy != 0 && (!following || !upcoming)) {
                    pageNumber++
                    refresh(pageNumber)
                }
            }
        })

        adapter = Adapter()
        recyclerView.adapter = adapter

        priceRangeSlider = findViewById(R.id.priceRangeSlider)
        priceTextView = findViewById(R.id.priceTextView)

        priceRangeSlider.addOnChangeListener { _, _, _ ->
            pageNumber = 1
            updatePrice()
        }

        priceRangeSlider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: RangeSlider) { }

            override fun onStopTrackingTouch(slider: RangeSlider) {
                refresh()
            }
        })




        checkboxUpcoming.setOnCheckedChangeListener { _, checked ->
            pageNumber = 1
            upcoming = checked
            if (following)
            {
                getFollowing(following)
            }else
            {
                refresh()
            }
        }


        checkboxFollowing.setOnCheckedChangeListener { _, checked ->
            pageNumber = 1
            following = checked
            getFollowing(following)
        }

        val priceCheckWorkRequest: WorkRequest =
            PeriodicWorkRequestBuilder<PriceCheckWorker>(
                sharedPreferences.getString("repeatInterval","4")!!.toLong(),TimeUnit.HOURS)
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
        if (following)
        {
            getFollowing(following)
        }else
        {
            refresh()
        }
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

    private fun refresh(pageNumber: Int = 1) {
            service.getSearchByName(
                sharedPreferences.getString("currency", "EUR") ?: "EUR",
                if (query.isBlank()) null else "like:$query",
                "between%3A${priceRangeSlider.values[0]}%2C${priceRangeSlider.values[1]}",
                if (upcoming) "in:upcoming" else null,
                page = pageNumber
            ).enqueue(object : Callback<Products> {
                override fun onResponse(call: Call<Products>, response: Response<Products>) {
                    Log.i(tag, "Call refresh body: ${call.request()}")
                    val responseBody = response.body()
                    if (response.isSuccessful && responseBody != null) {
                        println(responseBody.products)
                        adapter.setItems(responseBody.products, pageNumber != 1)
                    }
                }

                override fun onFailure(call: Call<Products>, t: Throwable) {
                    println(t.message)
                    Toast.makeText(this@MainActivity, "${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

    }

    private fun getFollowing(following : Boolean) {

        if (following)
        {
            val games = sGameViewModel.getAll()
            val gamesList:MutableList<Game> = mutableListOf()

            for (game in games) {
                var finalPrice: String
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://www.gog.com/products/prices?ids=${game.productId}&countryCode=SK&currency=${game.currency}")
                    .build()

                client.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onResponse(call: okhttp3.Call, responsePrice: okhttp3.Response) {
                        if (responsePrice.isSuccessful) {
                            val html = responsePrice.body()!!.string()
                            var json = html.substring(html.indexOf("cardProduct: ") + "cardProduct: ".length)
                            json = json.substring(0, json.indexOf("bonusWalletFunds"))
                            json = json.trim().dropLast(7)
                            json = json.substring(json.indexOf("\"finalPrice\":\""))
                            finalPrice = (json.split(":\"")[1].toInt()/100f).toString()
                            finalPrice = checkCurrencyReturnShort(game.currency) + finalPrice
                            Log.i(tag, "finalPrice: $finalPrice")

                            productIdService.getProductsByIds(
                                game.productId.toString()
                            ).enqueue(object : Callback<Game> {
                                override fun onResponse(call: Call<Game>, response: Response<Game>) {
                                    print(call.request().toString())

                                    val responseBody = response.body()
                                    if (response.isSuccessful && responseBody != null) {
                                        responseBody.price = Price(finalPrice,"0","0")
                                        Log.i(tag, "Following response body: $responseBody")
                                        gamesList.add(responseBody)
                                        adapter.setItems(gamesList)
                                    }
                                }

                                override fun onFailure(call: Call<Game>, t: Throwable) {
                                    Toast.makeText(this@MainActivity, "${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }

                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        Toast.makeText(this@MainActivity, "${e.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
        else{
            refresh()
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

    private fun checkCurrencyReturnShort(symbol: String) : String {
        if (symbol == "USD"){
            return "$"
        }
        if (symbol == "EUR"){
            Log.i("Currency", "Euro")
            return "€"
        }
        return ""
    }
}
