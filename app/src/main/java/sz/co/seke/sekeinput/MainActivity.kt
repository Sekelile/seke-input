package sz.co.seke.sekeinput

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.Toast
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.github.kittinunf.fuel.Fuel
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import sz.co.seke.sekeinput.R.string.base_url
import java.io.BufferedReader
import java.io.FileReader
import java.net.URISyntaxException

val MY_PERMISSIONS_REQUEST_CAMERA: Int = 1000;

class MainActivity : AppCompatActivity() {

    private var mSocket: Socket? = null


    private lateinit var codeScanner: CodeScanner
    private var itemList: MutableList<Item> = mutableListOf()
    private var serverIp = ""
    private lateinit var adapter: ItemsAdapter


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getServerIp()

        var code: String = ""
        var price: Double
        var quantity: Int
        var name: String

        adapter = ItemsAdapter(itemList)

        items_recycler_view.setHasFixedSize(true)
        items_recycler_view.layoutManager = LinearLayoutManager(this)
        items_recycler_view.adapter = adapter


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("Permission", "No")
            // Permission is not granted
        }
        

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    MY_PERMISSIONS_REQUEST_CAMERA
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {


            // Permission has already been granted
        }

        val scannerView = findViewById<CodeScannerView>(R.id.scanner_view)


        codeScanner = CodeScanner(this, scannerView)

        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,
        // ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                Toast.makeText(this, "Scan result: ${it.text}", Toast.LENGTH_LONG).show()
                scanner_view.visibility = View.GONE
                main_ly.visibility = View.VISIBLE
                scan_btn.visibility = View.GONE
                save_btn.visibility = View.VISIBLE
                code = it.text
            }
        }

        codeScanner.errorCallback = ErrorCallback {
            // or ErrorCallback.SUPPRESS
            runOnUiThread {
                Log.e("Error", it.toString())
                Toast.makeText(
                    this, "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }

        scan_btn.setOnClickListener {
            scanner_view.visibility = View.VISIBLE
            main_ly.visibility = View.GONE
            list.visibility = View.GONE
        }

        save_btn.setOnClickListener {
            val itemObject: JSONObject = JSONObject()
            itemObject.put("name", item_name_edit.text.toString())
            itemObject.put("bar_code", code)
            Log.e("Price", price_edit.toString())
            itemObject.put("price", price_edit.text.toString().toFloat())
            itemObject.put("quantity", quantity_text.text.toString().toInt())
            Fuel.post("${serverIp}/item")
                .jsonBody(itemObject.toString())
                .response { request, response, result ->
                    println(request)
                    println(response)
                    if (response.statusCode == 200) {
                        Toast.makeText(this, "Successful", Toast.LENGTH_LONG).show()
                        main_ly.visibility = View.GONE
                        save_btn.visibility = View.GONE
                        list.visibility = View.VISIBLE
                        scan_btn.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this, "There was an error " + response.statusCode, Toast.LENGTH_LONG).show()
                    }

                }
        }
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
                                mSocket = IO.socket(serverIp);
                                mSocket?.on("incwancwa", Emitter.Listener {
                                    Log.e("Inceancwa",it[0].toString())
                                })
                                mSocket?.on("item", Emitter.Listener {
                                    Log.e("EVENT",it[0].toString())
                                });
                                mSocket?.on("items", Emitter.Listener {
                                    Log.e("Items",it.toString())
                                    val its = JSONArray(it[0].toString())
                                    itemList.clear()
                                    for(i in 0..its.length()-1){
                                        val o = its.getJSONObject(i)
                                        itemList.add(Item(o.getString("bar_code"),o.getString("name"),o.getDouble("price"),o.getInt("quantity")))
                                    }
                                    runOnUiThread{
                                        adapter.notifyDataSetChanged()
                                    }

                                })
                                mSocket?.connect();
                            } catch (e: URISyntaxException) {
                                Log.e("SOCKETIO", e.toString())
                            }
                        }
                    }
                }
        }

    }
}
