package com.evora.technologies.office.engine.monetization

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the active foreground [Activity] without leaking references so
 * [PlayBillingClientGateway] can launch Google Play Billing UI cleanly behind
 * the Android-free [com.evora.technologies.office.core.domain.port.ProEntitlementPort] boundary.
 */
@Singleton
class ActiveActivityTracker @Inject constructor() : Application.ActivityLifecycleCallbacks {

    @Volatile
    private var currentRef: WeakReference<Activity>? = null

    fun currentActivity(): Activity? {
        val candidate = currentRef?.get() ?: return null
        return if (candidate.isFinishing || candidate.isDestroyed) null else candidate
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentRef = WeakReference(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        currentRef = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        currentRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentRef?.get() === activity) {
            currentRef = null
        }
    }
}
