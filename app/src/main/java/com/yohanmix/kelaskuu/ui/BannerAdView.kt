package com.yohanmix.kelaskuu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val isInEditMode = LocalInspectionMode.current
    if (isInEditMode) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("AdMob Banner Placeholder")
        }
    } else {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp), // Beri tinggi tetap agar pasti muncul
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-4785663785598276/5712519928"
                    
                    adListener = object : com.google.android.gms.ads.AdListener() {
                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            android.util.Log.e("AdMob", "Banner gagal dimuat: ${error.message}")
                        }
                        override fun onAdLoaded() {
                            android.util.Log.d("AdMob", "Banner berhasil dimuat!")
                        }
                    }

                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
