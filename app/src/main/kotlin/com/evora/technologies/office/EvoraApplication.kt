package com.evora.technologies.office

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.evora.technologies.office.core.data.worker.TrashPurgeWorker
import com.evora.technologies.office.engine.monetization.ActiveActivityTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EvoraApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var activeActivityTracker: ActiveActivityTracker

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activeActivityTracker)
        TrashPurgeWorker.schedulePeriodic(this)
    }
}
