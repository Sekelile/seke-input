package sz.co.seke.sekeinput

import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class ItemsAdapter (private val items: List<Item>) : RecyclerView.Adapter<ItemsAdapter.ItemViewholder>(){
    class ItemViewholder(var view: View) : RecyclerView.ViewHolder(view){
        val textView: TextView
        val icon: AppCompatImageView
        val priceView:TextView
        val quantity:TextView
        val _view = view

        init {
            textView = view.findViewById(R.id.action_title)
            icon = view.findViewById(R.id.action_icon)
            quantity = view.findViewById(R.id.quantity_view)
            priceView = view.findViewById(R.id.price_view)
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ItemViewholder {
        val actionLayout = LayoutInflater.from(p0.context)
            .inflate(R.layout.item_layout,p0,false);

        return ItemViewholder(actionLayout)
    }

    override fun getItemCount(): Int {

        return items.size
    }

    override fun onBindViewHolder(p0: ItemViewholder, p1: Int) {
        Log.e("title",items[p1].name)
        p0.textView.text = items[p1].name
        p0.priceView.text = "At E ${items[p1].price} Each"
        p0.quantity.text = "${items[p1].quantity} Units"
        p0._view.setOnClickListener({
            selectItem(it,items[p1])
        })

        p0.icon.setOnClickListener {
            p0.view
        }
    }

    fun selectItem(v:View,item:Item){

    }

}