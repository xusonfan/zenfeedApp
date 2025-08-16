package com.ddyy.zenfeed

import android.app.Application
import com.ddyy.zenfeed.data.FaviconManager

class ZenFeedApplication : Application() {

    val faviconManager: FaviconManager by lazy {
        FaviconManager(this)
    }
}