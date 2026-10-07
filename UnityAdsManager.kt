package com.spinearn.app

import android.app.Activity
import android.util.Log
import com.unity3d.ads.InitializationConfiguration
import com.unity3d.ads.InitializationListener
import com.unity3d.ads.LoadConfiguration
import com.unity3d.ads.RewardedAd
import com.unity3d.ads.RewardedLoadListener
import com.unity3d.ads.RewardedShowListener
import com.unity3d.ads.ShowConfiguration
import com.unity3d.ads.ShowFinishState
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsError

/**
 * Spinza rewarded-ad controller.
 *
 * The app preloads a rewarded ad, shows it after every completed wheel spin,
 * and exposes the reward only from Unity's onRewarded callback.
 */
class UnityAdsManager(
    private val activity: Activity,
    private val callback: Callback
) {
    interface Callback {
        fun onAdReady()
        fun onAdStarted()
        fun onRewardEarned()
        fun onAdClosedWithoutReward()
        fun onAdFailed(message: String)
    }

    companion object {
        private const val TAG = "SpinzaAds"
        private const val GAME_ID = "800390347"
        private const val REWARDED_PLACEMENT = "BP_Rewarded_Android"
        // Keep true while testing. Set false only after enabling production ads in Unity.
        private const val TEST_MODE = true
    }

    @Volatile private var initialized = false
    @Volatile private var loading = false
    @Volatile private var showing = false
    private var rewardedAd: RewardedAd? = null

    private val initializationListener = InitializationListener { error: UnityAdsError? ->
        if (error == null) {
            initialized = true
            Log.d(TAG, "Unity Ads initialized")
            preloadRewarded()
        } else {
            initialized = false
            callback.onAdFailed("Unity Ads initialization failed: ${error.message}")
            Log.e(TAG, "Initialization failed: ${error.message}")
        }
    }

    private val rewardedLoadListener = RewardedLoadListener { ad: RewardedAd?, error: UnityAdsError? ->
        loading = false
        if (ad == null) {
            rewardedAd = null
            callback.onAdFailed(error?.message ?: "Rewarded ad could not be loaded")
            return@RewardedLoadListener
        }

        rewardedAd = ad
        ad.onAdExpired = {
            rewardedAd = null
            Log.d(TAG, "Rewarded ad expired; preloading a fresh ad")
            preloadRewarded()
        }
        callback.onAdReady()
    }

    private val rewardedShowListener = object : RewardedShowListener {
        override fun onStarted(unityAd: RewardedAd) {
            showing = true
            callback.onAdStarted()
        }

        override fun onClicked(unityAd: RewardedAd) = Unit

        override fun onRewarded(unityAd: RewardedAd) {
            // This is the only callback used by Spinza to grant the ad-related unlock.
            callback.onRewardEarned()
        }

        override fun onCompleted(unityAd: RewardedAd, state: ShowFinishState) {
            showing = false
            rewardedAd = null
            if (state != ShowFinishState.COMPLETED) {
                callback.onAdClosedWithoutReward()
            }
            preloadRewarded()
        }

        override fun onFailed(unityAd: RewardedAd, error: UnityAdsError) {
            showing = false
            rewardedAd = null
            callback.onAdFailed(error.message ?: "Rewarded ad failed")
            preloadRewarded()
        }
    }

    fun initialize() {
        if (initialized) return
        val config = InitializationConfiguration.Builder(GAME_ID)
            .withTestMode(TEST_MODE)
            .build()
        UnityAds.initialize(config, initializationListener)
    }

    fun preloadRewarded() {
        if (!initialized || loading || showing || rewardedAd != null) return
        loading = true
        val loadConfig = LoadConfiguration.Builder(REWARDED_PLACEMENT).build()
        RewardedAd.load(loadConfig, rewardedLoadListener)
    }

    /** Shows a preloaded ad or loads one and shows it as soon as it is ready. */
    fun showRewarded() {
        if (!initialized) {
            callback.onAdFailed("Ads are still initializing. Please try again.")
            return
        }
        if (showing) return

        val ad = rewardedAd
        if (ad != null) {
            rewardedAd = null
            val showConfig = ShowConfiguration.Builder()
                .withCustomRewardString("spinza_spin_unlock")
                .build()
            ad.show(activity, showConfig, rewardedShowListener)
            return
        }

        if (loading) return
        loading = true
        val loadConfig = LoadConfiguration.Builder(REWARDED_PLACEMENT).build()
        RewardedAd.load(loadConfig, rewardedLoadListener)
    }
}
