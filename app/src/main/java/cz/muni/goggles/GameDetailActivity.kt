package cz.muni.goggles

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import cz.muni.goggles.database.SGame
import cz.muni.goggles.database.SGameViewModel
import cz.muni.goggles.database.SGameViewModelFactory
import cz.muni.goggles.databinding.ActivityGameDetailBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException


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
        val gameFromDatabase = sGameViewModel.getGame(slug)


        val gameTitle = intent.getStringExtra("title")
        this.title = gameTitle

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = title

        if (gameFromDatabase != null){
            binding.fab.setImageResource(R.drawable.ic_baseline_notifications_24)
            binding.fab.setColorFilter(Color.rgb(255,51,51))
        }

        binding.fab.setOnClickListener { view ->
            if (gameFromDatabase == null){
                val sGame = SGame(slug, gameTitle!!, intent.getIntExtra("productId",0),5, "EUR")
                sGameViewModel.insert(sGame)
                binding.fab.setImageResource(R.drawable.ic_baseline_notifications_24)
                binding.fab.setColorFilter(Color.rgb(255,51,51))
                Snackbar.make(view, "Game ${this.title} subscribed ", Snackbar.LENGTH_LONG).show()
            }else
            {
                sGameViewModel.delete(gameFromDatabase)
                binding.fab.setImageResource(R.drawable.ic_baseline_notifications_none_24)
                binding.fab.setColorFilter(Color.WHITE)
                Snackbar.make(view, "Game ${this.title} unsubscribed ", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
}