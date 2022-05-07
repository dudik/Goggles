package cz.muni.goggles.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import cz.muni.goggles.R
import cz.muni.goggles.activities.GameDetailActivity
import cz.muni.goggles.classes.Game


class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>()
{

    private val list = mutableListOf<Game>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_layout, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {

        val itemsViewModel = list[position]

        val urlFollowing = itemsViewModel.images?.logo2x?.replace(".jpg", ".png")
        if (urlFollowing == null)
        {
            Picasso.get().load(itemsViewModel.coverHorizontal?.replace(".png", "_product_tile_300w.png")).into(holder.imageView)
        }
        else
        {
            Picasso.get().load("https:$urlFollowing").into(holder.imageView)
        }

        holder.textView.text = itemsViewModel.title

        if (itemsViewModel.price?.final != null) holder.priceView.text = itemsViewModel.price!!.final
        else
        {
            holder.priceView.text = "Coming soon"
        }

        if (itemsViewModel.price?.discount != null && itemsViewModel.price?.discount != "0")
        {
            holder.discountView.text = itemsViewModel.price!!.discount
            holder.discountView.visibility = View.VISIBLE
        }
        else
        {
            holder.discountView.visibility = View.GONE
        }

        val context = holder.itemView.context

        holder.itemView.setOnClickListener {
            val i = Intent(context, GameDetailActivity::class.java)
            i.putExtra("title", itemsViewModel.title)
            i.putExtra("image", itemsViewModel.coverHorizontal)
            i.putExtra("slug", itemsViewModel.slug)
            i.putExtra("productId", itemsViewModel.id)
            i.putExtra("price", itemsViewModel.price?.final)
            context.startActivity(i)
        }
    }

    override fun getItemCount(): Int
    {
        return list.size
    }

    fun setItems(games: List<Game>, append: Boolean = false)
    {
        if (!append) list.clear()

        list.addAll(games)
        notifyDataSetChanged()
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView)
    {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val textView: TextView = itemView.findViewById(R.id.textView)
        val priceView: TextView = itemView.findViewById(R.id.textPrice)
        val discountView: TextView = itemView.findViewById(R.id.textDiscount)
    }
}