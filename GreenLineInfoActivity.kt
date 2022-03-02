package com.jeff.taichungmrt

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeff.taichungmrt.Function.getJSONFromPTX
import kotlinx.android.synthetic.main.activity_green_line_info.*
import kotlinx.android.synthetic.main.row_station_info.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class GreenLineInfoActivity : AppCompatActivity(), AnkoLogger {
  private val stationNameList = mutableListOf<String>()
  private val stationAddressList = mutableListOf<String>()
  private val infoURLList = mutableListOf<String>()

  private val SETTINGS = "settings"
  private val LENGUAGE = "lenguage"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_green_line_info)

    //從Google AdMob載入廣告
    adView_greenLineInfo.loadAd(AdRequest.Builder().build())

    doAsync {
      //從PTX「臺中捷運車站資料服務」API取得中英文車站名稱
      val apiURL =
        "https://ptx.transportdata.tw/MOTC/v2/Rail/Metro/Station/" +
        "TMRT?%24select=StationName&%24format=JSON"
      val stationNameJSON = getJSONFromPTX(apiURL)
      val stationNames =
        Gson().fromJson<List<StationName>>(stationNameJSON,
          object: TypeToken<List<StationName>>() {}.type)
      when (getSharedPreferences(SETTINGS, MODE_PRIVATE).getString(LENGUAGE, "繁體中文")) {
        "繁體中文" ->
          stationNames.forEach {
            stationNameList.add(it.StationName.Zh_tw)
          }
        "English" ->
          stationNames.forEach {
            stationNameList.add(it.StationName.En)
          }
      }

      //從本機取得車站地址及資訊網址
      var json =
        LocalJsonResolutionUtils
          .getJson(this@GreenLineInfoActivity, "臺中捷運綠線車站資訊.json")
      var greenLineStationInfos =
        Gson().fromJson<List<StationInfo>>(json, object : TypeToken<List<StationInfo>>() {}.type)
      greenLineStationInfos.forEach {
        stationAddressList.add(it.車站地址)
        infoURLList.add(it.資訊網址)
      }
      uiThread {
        recyclerView_greenLineInfo.layoutManager =
          LinearLayoutManager(this@GreenLineInfoActivity)
        recyclerView_greenLineInfo.setHasFixedSize(true)
        recyclerView_greenLineInfo.adapter = StationInfoAdapter()
      }
    }
  }

  inner class StationInfoAdapter(): RecyclerView.Adapter<StationInfoHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationInfoHolder {
      val view =
        LayoutInflater
          .from(parent.context).inflate(R.layout.row_station_info, parent, false)
      return StationInfoHolder(view)
    }

    override fun onBindViewHolder(holder: StationInfoHolder, position: Int) {
      holder.bindStationInfo(stationNameList.get(position),
        stationAddressList.get(position), infoURLList.get(position))
    }

    override fun getItemCount(): Int {
      return stationNameList.size
    }
  }

  inner class StationInfoHolder(view: View): RecyclerView.ViewHolder(view) {
    private var textView_stationNameInfo = view.textView_stationNameInfo
    private var textView_stationAddress = view.textView_stationAddress
    private var textView_infoURL = view.textView_infoURL

    init {
      //呼叫啟動Google地圖
      textView_stationAddress.setOnClickListener {
        val url = "https://www.google.com.tw/maps/place/${view.textView_stationAddress.text}"
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
      }

      //呼叫啟動使用者手機預設瀏覽器
      textView_infoURL.setOnClickListener {
        val url = view.textView_infoURL.text.toString()
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
      }
    }

    fun bindStationInfo(stationName: String, stationAddress: String, infoURL: String) {
      textView_stationNameInfo.text = stationName
      textView_stationAddress.text = stationAddress
      textView_infoURL.text = infoURL
    }
  }
}