package net.korul.hbbft

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.google.firebase.FirebaseApp
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import io.fabric.sdk.android.Fabric
import net.korul.hbbft.CoreHBBFT.CoreHBBFT

class DatabaseApplication : Application() {
    companion object {
        lateinit var instance: DatabaseApplication
            private set

        lateinit var mCoreHBBFT2X: CoreHBBFT
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FlowManager.init(FlowConfig.Builder(this).build())
        Fabric.with(this, Crashlytics())
        FirebaseApp.initializeApp(this)

        mCoreHBBFT2X = CoreHBBFT()
        mCoreHBBFT2X.Init(this)

//        FlowManager.getDatabase(AppDatabase::class.java).reset(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        FlowManager.destroy()
    }
}