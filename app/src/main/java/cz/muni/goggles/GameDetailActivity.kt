package cz.muni.goggles

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import cz.muni.goggles.databinding.ActivityGameDetailBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.*
import java.io.IOException


class GameDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameDetailBinding

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
                        runOnUiThread(Runnable {
                            findViewById<TextView>(R.id.descriptionView).text = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT)
                            val lay = findViewById<ImageView>(R.id.image_cover)
                            Picasso.get().load(image).into(lay)
                        })
                    }
                }
            })

        val message = intent.getStringExtra("title")

        this.setTitle(message)

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = title
        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }
}