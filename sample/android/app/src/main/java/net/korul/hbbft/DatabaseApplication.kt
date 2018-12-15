package net.korul.hbbft

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.google.firebase.FirebaseApp
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import io.fabric.sdk.android.Fabric
import net.korul.hbbft.CoreHBBFT.CoreHBBFT
import kotlin.concurrent.thread

class DatabaseApplication : Application() {
    companion object {
        lateinit var instance: DatabaseApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FlowManager.init(FlowConfig.Builder(this).build())
        Fabric.with(this, Crashlytics())
        FirebaseApp.initializeApp(this)

        val init = CoreHBBFT


//        thread {
//            val r = AutoRefreshOperation()
//            r.runSimulation()
//            val r2 = AutoRefreshOperation2()
//            r2.runSimulation()
//            ContentSendingTest.main(arrayOf())
//            ContentUpdatingTest.main(arrayOf())
//            NodeConnectionTest.main(arrayOf())
//            RefreshOperationTest.main(arrayOf())
//            RoutingTableSimulation.main(arrayOf())
//            RoutingTableStateTesting.main(arrayOf())
//            SaveStateTest.main(arrayOf())
//            SaveStateTest2.main(arrayOf())
//            SimpleMessageTest.main(arrayOf())
//        }


//        FlowManager.getDatabase(AppDatabase::class.java).reset(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        FlowManager.destroy()
    }
}