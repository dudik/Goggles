package cz.muni.goggles.activities

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.squareup.picasso.Picasso
import cz.muni.goggles.R
import cz.muni.goggles.SGameApplication
import cz.muni.goggles.database.SGame
import cz.muni.goggles.database.SGameViewModel
import cz.muni.goggles.database.SGameViewModelFactory
import cz.muni.goggles.databinding.ActivityGameDetailBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import android.content.Intent
import android.net.Uri


class GameDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameDetailBinding
    private val sGameViewModel: SGameViewModel by viewModels {
        SGameViewModelFactory((application as SGameApplication).repository)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val client = OkHttpClient()

        println("https://www.gog.com/en/game/" + intent.getStringExtra("slug"))
        val request = Request.Builder()
            .url("https://www.gog.com/en/game/" + intent.getStringExtra("slug")!!.replace("-", "_"))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
                println("CHYBA")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.body() != null) {
                    val html = response.body()!!.string()

                    var json = html.substring(html.indexOf("cardProduct: ") + "cardProduct: ".length)
                    println(json)
                    json = json.substring(0, json.indexOf("cardProductId:"))
                    json = json.trim().dropLast(1)

                    val gameDetail = JSONObject(json)
                    val description = gameDetail.getString("description")
                    val image = gameDetail.get("galaxyBackgroundImage").toString()
                    runOnUiThread {
                        findViewById<TextView>(R.id.descriptionView).text = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        val lay = findViewById<ImageView>(R.id.image_cover)
                        Picasso.get().load(image).into(lay)
                    }
                }
            }
        })

        var slug = ""
        if (!intent.getStringExtra("slug").isNullOrEmpty()){
            slug = intent.getStringExtra("slug")!!
        }

        Log.i("DetailLog","Slug detail: $slug")

        var gameFromDatabase = sGameViewModel.getGame(slug)

        Log.i("DetailLog","gameFromDatabase: $gameFromDatabase")

        val gameTitle = intent.getStringExtra("title")
        this.title = gameTitle

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = title
        binding.priceText.text = intent.getStringExtra("price")

        binding.fab2.setOnClickListener {
            var url = "https://www.gog.com/game/" + slug.replace("-", "_")
            val blankIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(blankIntent)
        }

        if (gameFromDatabase != null){
            binding.fab.setImageResource(R.drawable.ic_baseline_notifications_24)
            binding.fab.setColorFilter(Color.rgb(255,51,51))
        }

        binding.fab.setOnClickListener {
            gameFromDatabase = sGameViewModel.getGame(slug)
            if (gameFromDatabase == null){
                selectPriceForAlert(slug, gameTitle!!)
            }else
            {
                sGameViewModel.delete(gameFromDatabase!!)
                binding.fab.setImageResource(R.drawable.ic_baseline_notifications_none_24)
                binding.fab.setColorFilter(Color.WHITE)
                Toast.makeText(this, "Game $gameTitle unsubscribed", Toast.LENGTH_LONG).show()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun selectPriceForAlert(slug : String, gameTitle: String) {
        var selectedPrice = 0
        val dialogBuilder = AlertDialog.Builder(this)
        val pricePopupView = layoutInflater.inflate(R.layout.popup,null)
        dialogBuilder.setView(pricePopupView)
        val dialog = dialogBuilder.create()
        dialog.show()

        val spinner = pricePopupView.findViewById<Spinner>(R.id.spinner)

        val currency = intent.getStringExtra("price")!!.first()

        val listOfItems = arrayOf("5 $currency", "10 $currency", "15 $currency", "20 $currency", "25 $currency", "30 $currency", "35 $currency", "40 $currency")
        val arrayAdapter = ArrayAdapter(this, R.layout.custom_spinner, listOfItems)
        arrayAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown)
        spinner!!.adapter = arrayAdapter


        spinner.adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPrice = adapterView?.getItemAtPosition(position).toString().dropLast(2).toInt()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }

        pricePopupView.findViewById<Button>(R.id.okButton).setOnClickListener{ view ->
            val sGame = SGame(slug.replace("-","_"), gameTitle, intent.getIntExtra("productId",0),selectedPrice, checkCurrencyReturnLong(currency))
            sGameViewModel.insert(sGame)
            binding.fab.setImageResource(R.drawable.ic_baseline_notifications_24)
            binding.fab.setColorFilter(Color.rgb(255,51,51))
            Toast.makeText(this, "Game $gameTitle subscribed", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }
    }

    private fun checkCurrencyReturnLong(symbol: Char) : String {
        if (symbol == '$'){
            Log.i("Currency", "Dollar")
            return "USD"
        }
        if (symbol == '€'){
            Log.i("Currency", "Euro")
            return "EUR"
        }
        Log.i("Currency", "Empty")
        return ""
    }

}