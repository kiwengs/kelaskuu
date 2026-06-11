package com.yohanmix.kelaskuu.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.yohanmix.kelaskuu.R

@Composable
fun NativeAdViewComposable(modifier: Modifier = Modifier) {
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    val context = LocalContext.current
    
    val adLoader = remember {
        AdLoader.Builder(context, "ca-app-pub-4785663785598276/5612986578") // Real ID
            .forNativeAd { ad ->
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Handle error
                }
            })
            .build()
    }

    LaunchedEffect(Unit) {
        adLoader.loadAd(AdRequest.Builder().build())
    }

    if (nativeAd != null) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            factory = { ctx ->
                val adView = LayoutInflater.from(ctx)
                    .inflate(R.layout.ad_unified, null) as NativeAdView
                populateNativeAdView(nativeAd!!, adView)
                adView
            },
            update = { adView ->
                populateNativeAdView(nativeAd!!, adView)
            }
        )
    }
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.iconView = adView.findViewById(R.id.ad_app_icon)
    adView.priceView = adView.findViewById(R.id.ad_price)
    adView.starRatingView = adView.findViewById(R.id.ad_stars)
    adView.storeView = adView.findViewById(R.id.ad_store)
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

    (adView.headlineView as TextView).text = nativeAd.headline
    
    if (nativeAd.body == null) {
        adView.bodyView?.visibility = View.GONE
    } else {
        adView.bodyView?.visibility = View.VISIBLE
        (adView.bodyView as TextView).text = nativeAd.body
    }

    if (nativeAd.callToAction == null) {
        adView.callToActionView?.visibility = View.GONE
    } else {
        adView.callToActionView?.visibility = View.VISIBLE
        (adView.callToActionView as Button).text = nativeAd.callToAction
    }

    if (nativeAd.icon == null) {
        adView.iconView?.visibility = View.GONE
    } else {
        (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
        adView.iconView?.visibility = View.VISIBLE
    }

    if (nativeAd.price == null) {
        adView.priceView?.visibility = View.GONE
    } else {
        adView.priceView?.visibility = View.VISIBLE
        (adView.priceView as TextView).text = nativeAd.price
    }

    if (nativeAd.store == null) {
        adView.storeView?.visibility = View.GONE
    } else {
        adView.storeView?.visibility = View.VISIBLE
        (adView.storeView as TextView).text = nativeAd.store
    }

    if (nativeAd.starRating == null) {
        adView.starRatingView?.visibility = View.GONE
    } else {
        (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
        adView.starRatingView?.visibility = View.VISIBLE
    }

    if (nativeAd.advertiser == null) {
        adView.advertiserView?.visibility = View.GONE
    } else {
        (adView.advertiserView as TextView).text = nativeAd.advertiser
        adView.advertiserView?.visibility = View.VISIBLE
    }

    adView.setNativeAd(nativeAd)
}
