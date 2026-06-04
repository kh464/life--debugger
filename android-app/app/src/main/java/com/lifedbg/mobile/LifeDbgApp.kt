package com.lifedbg.mobile

import android.app.Application
import com.lifedbg.mobile.db.LifeDbgDatabase
import com.lifedbg.mobile.worker.UsageCollectWorker

class LifeDbgApp : Application() {
    val database: LifeDbgDatabase by lazy {
        LifeDbgDatabase.create(this)
    }

    override fun onCreate() {
        super.onCreate()
        UsageCollectWorker.schedule(this)
    }
}
