package com.example.migo

import android.app.Application
import com.example.migo.data.remote.NvidiaCareerService
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MigoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NvidiaCareerService.initialize(this)
        PDFBoxResourceLoader.init(applicationContext)
    }
}
