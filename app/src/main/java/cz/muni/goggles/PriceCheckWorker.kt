package cz.muni.goggles

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import cz.muni.goggles.database.SGameDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.CountDownLatch

class PriceCheckWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {

    private val channelId = "channelID"
    private val notificationId = 0

    val tag = "PriceCheckWorker"
    val context = appContext
    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun doWork(): Result {

        Log.i(tag, "PriceCheckWorker working")
        val finalGames = mutableListOf<String>()

        val client = OkHttpClient()

        val gamesForCheck = SGameDatabase.getDatabase(context,applicationScope).sGameDao().getAll()
        val countDownLatch = CountDownLatch(gamesForCheck.size)
        for (game in gamesForCheck)
        {
            Log.i(tag, "https://www.gog.com/products/prices?ids=${game.productId}&countryCode=SK&currency=${game.currency}")
            Log.i(tag, "ProductId from DB: ${game.productId}")

            val request = Request.Builder()
                .url("https://www.gog.com/products/prices?ids=${game.productId}&countryCode=SK&currency=${game.currency}")
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    e.printStackTrace()
                    Log.e(tag, "Error on call")
                    countDownLatch.countDown()
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (response.body() != null) {
                        val html = response.body()!!.string()
                        var json = html.substring(html.indexOf("cardProduct: ") + "cardProduct: ".length)

                        json = json.substring(0, json.indexOf("bonusWalletFunds"))

                        json = json.trim().dropLast(7)
                        json = json.substring(json.indexOf("\"finalPrice\":\""))
                        val finalPrice = json.split(":\"")[1].toInt()
                        Log.i(tag, "finalPrice: $finalPrice")

                        if (finalPrice < game.price*100)
                        {
                            finalGames.add(game.name)
                            Log.i(tag, "Game Added")
                        }
                    }
                    countDownLatch.countDown()
                }
            })
        }
        countDownLatch.await()

        Log.i(tag, "Is Empty: ${finalGames.isNotEmpty()}")
        Log.i(tag, "Is Empty: ${finalGames.joinToString(", ")}")
        if (finalGames.isNotEmpty()) {
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle("Great price")
                .setContentText("Buy ${finalGames.joinToString(", ") } now")
                .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId,notification)
        }

        return Result.success()
    }
}