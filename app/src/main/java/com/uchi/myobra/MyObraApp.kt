package com.uchi.myobra

import android.app.Application
import com.uchi.myobra.util.ThemeManager

class MyObraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.init(this)
    }
}
