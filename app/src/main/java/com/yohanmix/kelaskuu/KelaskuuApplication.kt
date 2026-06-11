package com.yohanmix.kelaskuu

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KelaskuuApplication : Application() {

    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        
        // Inisialisasi AdMob secara sinkron di Main Thread untuk memastikan siap digunakan
        MobileAds.initialize(this) { status ->
            android.util.Log.d("AdMob", "AdMob Initialized: $status")
            appOpenAdManager.loadAd()
        }

        appOpenAdManager = AppOpenAdManager(this)
    }
}
