package com.jeff.taichungmrt

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import com.google.android.gms.ads.AdRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeff.taichungmrt.Function.getJSONFromPTX
import kotlinx.android.synthetic.main.activity_green_line_travel_time.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class GreenLineTravelTimeActivity : AppCompatActivity(), AnkoLogger {
  private val SETTINGS = "settings"
  private val LENGUAGE = "lenguage"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_green_line_travel_time)

    //從Google AdMob載入廣告
    adView_greenLineTravelTime.loadAd(AdRequest.Builder().build())

    //選單功能
    toolBar_greenLineTravelTime.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.menuItem_stationInfo -> {
          startActivity(Intent(this, GreenLineInfoActivity::class.java))
          true
        }
        R.id.menuItem_network -> {
          startActivity(Intent(this, NetworkActivity::class.java))
          true
        }
        else -> false
      }
    }

    //從PTX取得車站名稱，並設定Spinner
    doAsync {
      val stationNameAPIURL =
        "https://ptx.transportdata.tw/MOTC/v2/Rail/Metro/Station/" +
        "TMRT?%24select=StationName&%24format=JSON"
      val stationNameJSON = getJSONFromPTX(stationNameAPIURL)
      val stationNames =
        Gson().fromJson<List<StationName>>(stationNameJSON,
          object: TypeToken<List<StationName>>() {}.type)
      val beitunMainStationToHSRTaichungStationList = mutableListOf<String>()

      val stationNameDescAPIURL =
        "https://ptx.transportdata.tw/MOTC/v2/Rail/Metro/Station/" +
        "TMRT?%24select=StationID%2CStationName&%24orderby=StationID%20desc&%24format=JSON"
      val stationNameDescJSON = getJSONFromPTX(stationNameDescAPIURL)
      val stationNamesDesc =
        Gson().fromJson<List<StationNameDesc>>(stationNameDescJSON,
          object: TypeToken<List<StationNameDesc>>() {}.type)
      val hsrTaichungStationToBeitunMainStationList = mutableListOf<String>()
      when (getSharedPreferences(SETTINGS, MODE_PRIVATE).getString(LENGUAGE, "繁體中文")) {
        "繁體中文" -> {
          stationNames.forEach {
            beitunMainStationToHSRTaichungStationList.add(it.StationName.Zh_tw)
          }
          stationNamesDesc.forEach {
            hsrTaichungStationToBeitunMainStationList.add(it.StationName.Zh_tw)
          }
        }
        "English" -> {
          stationNames.forEach {
            beitunMainStationToHSRTaichungStationList.add(it.StationName.En)
          }
          stationNamesDesc.forEach {
            hsrTaichungStationToBeitunMainStationList.add(it.StationName.En)
          }
        }
      }
      uiThread {
        spinner_initialStation.adapter =
          ArrayAdapter(this@GreenLineTravelTimeActivity,
          android.R.layout.simple_spinner_dropdown_item, beitunMainStationToHSRTaichungStationList)
        spinner_terminalStation.adapter =
          ArrayAdapter(this@GreenLineTravelTimeActivity,
          android.R.layout.simple_spinner_dropdown_item, hsrTaichungStationToBeitunMainStationList)
      }
    }

    //從PTX取得乘車時間及各種票票價
    doAsync {
      val apiURL =
        "https://ptx.transportdata.tw/MOTC/v2/Rail/Metro/ODFare/" +
        "TMRT?%24select=OriginStationName%2CDestinationStationName%2CFares%2CTravelTime&" +
        "%24format=JSON"
      val fareJSON = getJSONFromPTX(apiURL)
      val fares = Gson().fromJson<List<Fare>>(fareJSON, object: TypeToken<List<Fare>>() {}.type)
      val language =
        getSharedPreferences(SETTINGS, MODE_PRIVATE).getString(LENGUAGE, "繁體中文")
      btn_confirm.setOnClickListener {
        fares.forEach {
          var originStationName = ""
          var destinationStationName = ""
          when (language) {
            "繁體中文" -> {
              originStationName = it.OriginStationName.Zh_tw
              destinationStationName = it.DestinationStationName.Zh_tw
            }
            "English" -> {
              originStationName = it.OriginStationName.En
              destinationStationName = it.DestinationStationName.En
            }
          }
          if (spinner_initialStation.selectedItem == originStationName &&
            spinner_terminalStation.selectedItem == destinationStationName) {
            var boardingTime = "${it.TravelTime}"
            var singleTripTicketPrice = ""
            var singleJourneyTicketSeniorPrice = ""
            var electronicTicketPrice = ""
            it.Fares.forEach {
              if (it.TicketType == 1 && it.FareClass == 1) singleTripTicketPrice = "$${it.Price}"
                else if (it.TicketType == 1 && it.FareClass == 4)
                  singleJourneyTicketSeniorPrice = "$${it.Price}"
                else if (it.TicketType == 3 && it.FareClass == 1)
                  electronicTicketPrice = "$${it.Price}"
              uiThread {
                textView_boardingTime.text = boardingTime
                textView_singleTripTicketPrice.text = singleTripTicketPrice
                textView_singleJourneyTicketSeniorPrice.text = singleJourneyTicketSeniorPrice
                textView_electronicTicketPrice.text = electronicTicketPrice
              }
            }
          }
        }
      }
    }
  }
}