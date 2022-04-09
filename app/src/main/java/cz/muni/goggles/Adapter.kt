package cz.muni.goggles

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso


class Adapter(private val list : List<Game>) : RecyclerView.Adapter<Adapter.ViewHolder>() {
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
        holder.priceView.text = ItemsViewModel.price.final

        if (ItemsViewModel.price.discount != null) {
            holder.discountView.text = ItemsViewModel.price.discount
            holder.discountView.visibility = View.VISIBLE
        } else {
            holder.discountView.visibility = View.GONE
        }

        val context = holder.itemView.getContext()

        holder.itemView.setOnClickListener {
            val i = Intent(context, GameDetailActivity::class.java)
            i.putExtra("title", ItemsViewModel.title)
            i.putExtra("image", ItemsViewModel.coverHorizontal)
            i.putExtra("slug", ItemsViewModel.slug)
            context.startActivity(i)
        }

        println(ItemsViewModel.title)

    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val textView: TextView = itemView.findViewById(R.id.textView)
        val priceView: TextView = itemView.findViewById(R.id.textPrice)
        val discountView: TextView = itemView.findViewById(R.id.textDiscount)
    }
}