package br.com.fiap.letssign

import android.app.Application
import br.com.fiap.letssign.data.local.DatabaseSeeder
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LetsSignApp : Application() {

    @Inject lateinit var seeder: DatabaseSeeder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext) // prepara a biblioteca de PDF
        appScope.launch { seeder.seedIfEmpty() }
    }
}