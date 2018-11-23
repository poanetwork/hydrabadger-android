package net.korul.hbbft

import android.app.Application
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import net.korul.hbbft.common.data.model.coreDataBase.AppDatabase


class DatabaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FlowManager.init(FlowConfig.Builder(this).build())
        FlowManager.getDatabase(AppDatabase::class.java).reset(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        FlowManager.destroy()
    }
}