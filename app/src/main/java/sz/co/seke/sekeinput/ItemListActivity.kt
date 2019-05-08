package sz.co.seke.sekeinput

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.support.v4.app.NavUtils
import android.support.v7.app.ActionBar
import android.text.format.Formatter
import android.util.Log
import android.view.MenuItem
import com.github.kittinunf.fuel.Fuel
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter

import sz.co.seke.sekeinput.dummy.DummyContent
import kotlinx.android.synthetic.main.activity_item_list.*
import kotlinx.android.synthetic.main.item_list_content.view.*
import kotlinx.android.synthetic.main.item_list.*
import org.json.JSONArray
import java.io.BufferedReader
import java.io.FileReader
import java.net.URISyntaxException

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [ItemDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class ItemListActivity : AppCompatActivity() {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false
    private var mSocket: Socket? = null
    private var salesList:ArrayList<Item> = arrayListOf()

    private var serverIp = ""

    private val adapter = SimpleItemRecyclerViewAdapter(this, salesList, twoPane)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)
        getServerIp()

        setSupportActionBar(toolbar)
        toolbar.title = title

        //fab.visibility = View.GONE
        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (item_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }

        title = "Sales"

        setupRecyclerView(item_list)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = adapter
    }

    class SimpleItemRecyclerViewAdapter(
        private val parentActivity: ItemListActivity,
        private val values: List<Item>,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as DummyContent.DummyItem
                if (twoPane) {
                    val fragment = ItemDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(ItemDetailFragment.ARG_ITEM_ID, item.id)
                        }
                    }
                    parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.item_detail_container, fragment)
                        .commit()
                } else {
                    val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                        putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.quantity.text = item.quantity.toString()
            holder.name.text = "${item.name}"
            holder.bought.text = item.bought.toString()

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val quantity: TextView = view.quantity
            val name: TextView = view.name
            val bought: TextView = view.bought
        }
    }


    fun getItems(){
        mSocket = IO.socket(serverIp);
        Fuel.get("$serverIp/item")
            .response{ request, response, result ->
                Log.e("Getting items",String(response.data))
                val its = JSONArray(String(response.data))
                salesList.clear()
                for(i in 0..its.length()-1){
                    val o = its.getJSONObject(i)
                    if(o.has("bought")){
                        salesList.add(Item(o.getString("bar_code"),o.getString("name"),o.getDouble("price"),o.getInt("quantity"),o.getInt("bought")))
                    }else{
                        salesList.add(Item(o.getString("bar_code"),o.getString("name"),o.getDouble("price"),o.getInt("quantity"),0))
                    }
                }

                adapter.notifyDataSetChanged()

            }

        mSocket?.on("items", Emitter.Listener {
            Log.e("Items",it.toString())
            val its = JSONArray(it[0].toString())
            salesList.clear()
            for(i in 0..its.length()-1){
                val o = its.getJSONObject(i)
                if(o.has("bought")){
                    salesList.add(Item(o.getString("bar_code"),o.getString("name"),o.getDouble("price"),o.getInt("quantity"),o.getInt("bought")))
                }else{
                    salesList.add(Item(o.getString("bar_code"),o.getString("name"),o.getDouble("price"),o.getInt("quantity"),0))
                }
            }
            runOnUiThread{
                adapter.notifyDataSetChanged()
            }

        })
        mSocket?.connect();
    }



    fun getServerIp() {
        val BUF: Int = 8 * 1024
        val bufferedReader = BufferedReader(FileReader("/proc/net/arp"), BUF)
        var line: String? = bufferedReader.readLine()
        val wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiMgr.connectionInfo
        val ip = wifiInfo.ipAddress
        val ipAddress = Formatter.formatIpAddress(ip)

        val base = ipAddress.substring(0,ipAddress.lastIndexOf("."))
        for(i in 1..254){
            Fuel.get("http://$base.$i:3002/ping")
                .response{ request, response, result ->
                    if(response.statusCode == 200){
                        Log.e("HOST",response.url.host)
                        serverIp = "http://${response.url.host}:3002"
                        if (serverIp.length > 1) {
                            try {
                                getItems()
                            } catch (e: URISyntaxException) {
                                Log.e("SOCKETIO", e.toString())
                            }
                        }
                    }
                }
        }

    }
}
