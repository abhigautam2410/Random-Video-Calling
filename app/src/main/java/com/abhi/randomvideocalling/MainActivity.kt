package com.abhi.randomvideocalling

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    lateinit var btnGetStart : Button
    lateinit var adView : AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adView = findViewById(R.id.adView)



        MobileAds.initialize(this){ }
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        btnGetStart = findViewById(R.id.btnGetStart)
        btnGetStart.setOnClickListener {
            val intent = Intent(this,VideoCallActivity::class.java)
            startActivity(intent)
        }
    }
}