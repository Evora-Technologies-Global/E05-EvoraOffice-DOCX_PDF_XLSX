package com.evora.technologies.office.engine.monetization

import android.content.Context
import com.evora.technologies.office.core.data.di.MainDispatcher
import com.evora.technologies.office.core.domain.port.ConsentPort
import com.evora.technologies.office.core.model.ConsentState
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages Google User Messaging Platform (UMP SDK 4.0.0) consent gathering and
 * EEA/UK/CH privacy revocation (US-002 / T-024).
 */
@Singleton
class UmpConsentManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val activityTracker: ActiveActivityTracker,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : ConsentPort {

    private val consentMutex = Mutex()
    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(context)
    }

    private val _consentState = MutableStateFlow(readCurrentSnapshot(initialized = false))
    override val consentState: StateFlow<ConsentState> = _consentState.asStateFlow()

    override suspend fun gatherConsentIfNeeded(): ConsentState = consentMutex.withLock {
        val activity = activityTracker.currentActivity()
        if (activity == null) {
            val snapshot = readCurrentSnapshot(initialized = true)
            _consentState.value = snapshot
            return@withLock snapshot
        }

        withContext(mainDispatcher) {
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            suspendCancellableCoroutine { cont ->
                try {
                    consentInformation.requestConsentInfoUpdate(
                        activity,
                        params,
                        {
                            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                                val next = readCurrentSnapshot(initialized = true)
                                _consentState.value = next
                                if (cont.isActive) cont.resume(next)
                            }
                        },
                        {
                            val fallback = readCurrentSnapshot(initialized = true)
                            _consentState.value = fallback
                            if (cont.isActive) cont.resume(fallback)
                        },
                    )
                } catch (_: Throwable) {
                    val fallback = readCurrentSnapshot(initialized = true)
                    _consentState.value = fallback
                    if (cont.isActive) cont.resume(fallback)
                }
            }
        }
    }

    override suspend fun showPrivacyOptionsForm(): Result<ConsentState> = consentMutex.withLock {
        val activity = activityTracker.currentActivity()
            ?: return@withLock Result.failure(
                IllegalStateException("No active window available to open privacy choices"),
            )

        withContext(mainDispatcher) {
            suspendCancellableCoroutine { cont ->
                try {
                    UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                        val next = readCurrentSnapshot(initialized = true)
                        _consentState.value = next
                        if (!cont.isActive) return@showPrivacyOptionsForm
                        if (formError != null) {
                            cont.resume(Result.failure(IllegalStateException(formError.message)))
                        } else {
                            cont.resume(Result.success(next))
                        }
                    }
                } catch (t: Throwable) {
                    if (cont.isActive) cont.resume(Result.failure(t))
                }
            }
        }
    }

    private fun readCurrentSnapshot(initialized: Boolean): ConsentState = runCatching {
        ConsentState(
            canRequestAds = consentInformation.canRequestAds(),
            privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            isInitialized = initialized,
        )
    }.getOrElse {
        ConsentState(
            canRequestAds = false,
            privacyOptionsRequired = false,
            isInitialized = initialized,
        )
    }
}
