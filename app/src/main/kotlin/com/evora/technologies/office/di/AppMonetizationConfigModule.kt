package com.evora.technologies.office.di

import com.evora.technologies.office.BuildConfig
import com.evora.technologies.office.engine.monetization.MonetizationRuntimeConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppMonetizationConfigModule {

    @Provides
    @Singleton
    fun provideMonetizationRuntimeConfig(): MonetizationRuntimeConfig = MonetizationRuntimeConfig(
        isDebugBuild = BuildConfig.DEBUG,
        admobAppId = BuildConfig.ADMOB_APP_ID,
        interstitialUnitId = BuildConfig.ADMOB_INTERSTITIAL_ID,
        appOpenUnitId = BuildConfig.ADMOB_APP_OPEN_ID,
        rewardedUnitId = BuildConfig.ADMOB_REWARDED_ID,
        nativeUnitId = BuildConfig.ADMOB_NATIVE_ID,
        adjustAppToken = BuildConfig.ADJUST_APP_TOKEN,
    )
}
