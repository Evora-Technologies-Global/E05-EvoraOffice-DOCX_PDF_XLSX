package com.evora.technologies.office

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.data.worker.TrashPurgeWorker
import com.evora.technologies.office.engine.monetization.ActiveActivityTracker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EvoraApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var activeActivityTracker: ActiveActivityTracker

    @field:IoDispatcher
    @Inject
    lateinit var ioDispatcher: CoroutineDispatcher

    private val appScope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activeActivityTracker)
        // Schedule WorkManager off the main thread to preserve < 1.2s cold start (T-025)
        appScope.launch {
            TrashPurgeWorker.schedulePeriodic(applicationContext)
        }
    }
}
