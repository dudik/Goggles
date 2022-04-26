package cz.muni.goggles.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import cz.muni.goggles.classes.Game
import cz.muni.goggles.R
import cz.muni.goggles.activities.GameDetailActivity


class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

    private val list = mutableListOf<Game>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_layout, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = list[position]

        Picasso.get().load(ItemsViewModel.coverHorizontal.replace(".png", "_product_tile_300w.png")).into(holder.imageView)
        println(ItemsViewModel.coverHorizontal)

        holder.textView.text = ItemsViewModel.title

        if (ItemsViewModel.price != null)
            holder.priceView.text = ItemsViewModel.price.final
        else {
            holder.priceView.text = "Coming soon"
        }

        if (ItemsViewModel.price?.discount != null) {
            holder.discountView.text = ItemsViewModel.price.discount
            holder.discountView.visibility = View.VISIBLE
        } else {
            holder.discountView.visibility = View.GONE
        }

        val context = holder.itemView.context

        holder.itemView.setOnClickListener {
            val i = Intent(context, GameDetailActivity::class.java)
            i.putExtra("title", ItemsViewModel.title)
            i.putExtra("image", ItemsViewModel.coverHorizontal)
            i.putExtra("slug", ItemsViewModel.slug)
            i.putExtra("productId", ItemsViewModel.id)
            i.putExtra("price", ItemsViewModel.price.final)
            context.startActivity(i)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setItems(games: List<Game>) {
        list.clear()
        list.addAll(games)
        notifyDataSetChanged()
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val textView: TextView = itemView.findViewById(R.id.textView)
        val priceView: TextView = itemView.findViewById(R.id.textPrice)
        val discountView: TextView = itemView.findViewById(R.id.textDiscount)
    }
}