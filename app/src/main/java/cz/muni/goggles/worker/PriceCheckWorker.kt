package cz.muni.goggles.worker

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import cz.muni.goggles.R
import cz.muni.goggles.activities.MainActivity
import cz.muni.goggles.database.SGameDatabase
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch

class PriceCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val channelId = "goggles"
    private val notificationId = 0
    val context = appContext

    override fun doWork(): Result {

        val finalGames = mutableListOf<String>()
        val client = OkHttpClient()
        val gamesForCheck = SGameDatabase.getDatabase(context).sGameDao().getAll()
        val countDownLatch = CountDownLatch(gamesForCheck.size)

        for (game in gamesForCheck) {

            val request = Request.Builder()
                .url("https://www.gog.com/products/prices?ids=${game.productId}&countryCode=SK&currency=${game.currency}")
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    e.printStackTrace()
                    countDownLatch.countDown()
                }

                override fun onResponse(call: okhttp3.Call, response: Response)
                {
                    if (response.body() != null)
                    {
                        val finalPrice = extractPriceString(response)
                        if (finalPrice < game.price * 100)
                        {
                            finalGames.add(game.name)
                        }
                    }
                    countDownLatch.countDown()
                }
            })
        }
        countDownLatch.await()

        createNotification(finalGames)

        return Result.success()
    }

    private fun extractPriceString(response: Response): Int
    {
        val html = response.body()!!.string()
        var json = html.substring(html.indexOf("cardProduct: ") + "cardProduct: ".length)
        json = json.substring(0, json.indexOf("bonusWalletFunds"))
        json = json.trim().dropLast(7)
        json = json.substring(json.indexOf("\"finalPrice\":\""))
        return json.split(":\"")[1].toInt()
    }

    private fun createNotification(finalGames: MutableList<String>)
    {
        val resultIntent = Intent(context, MainActivity::class.java)
        resultIntent.putExtra("fromNotification", true)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(
                0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        if (finalGames.isNotEmpty())
        {
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentIntent(resultPendingIntent)
                .setContentTitle("Great price")
                .setContentText("Buy ${finalGames.joinToString(", ")} now")
                .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, notification)
            }
        }
    }
}