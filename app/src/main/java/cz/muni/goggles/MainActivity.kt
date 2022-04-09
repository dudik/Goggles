package cz.muni.goggles

import android.os.Bundle
import android.view.Menu
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.*
import retrofit2.converter.moshi.MoshiConverterFactory


class MainActivity : AppCompatActivity() {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://catalog.gog.com/v1/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val service: Api = retrofit.create(Api::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val result = service.getSearchByName("").
        enqueue(object : Callback<Products> {
            override fun onResponse(call: Call<Products>, response: Response<Products>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    println(responseBody.products)

                    val recyclerview = findViewById<RecyclerView>(R.id.recycler)
                    recyclerview.layoutManager = GridLayoutManager(this@MainActivity, 2)

                    val adapter = Adapter(responseBody.products)

                    recyclerview.adapter = adapter
                }
            }

            override fun onFailure(call: Call<Products>, t: Throwable) {
                Toast.makeText(this@MainActivity, "${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_scrolling, menu)

        val search = menu?.findItem(R.id.nav_search)
        val searchView = search?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                val result = service.getSearchByName("like:" + p0).
                enqueue(object : Callback<Products> {
                    override fun onResponse(call: Call<Products>, response: Response<Products>) {
                        val responseBody = response.body()
                        if (response.isSuccessful && responseBody != null) {
                            println(responseBody.products)

                            val recyclerview = findViewById<RecyclerView>(R.id.recycler)
                            recyclerview.layoutManager = GridLayoutManager(this@MainActivity, 2)

                            val adapter = Adapter(responseBody.products)

                            recyclerview.adapter = adapter
                        }
                    }

                    override fun onFailure(call: Call<Products>, t: Throwable) {
                        Toast.makeText(this@MainActivity, "${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })

                return false;
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false;
            }

        })

        return super.onCreateOptionsMenu(menu)
    }
}