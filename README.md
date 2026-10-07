# Spinza Android — v2

A native Android Spin & Earn prototype using Unity Ads Rewarded.

## Exact flow
1. Tap **SPIN NOW**.
2. Wheel animates and selects a virtual-coin prize.
3. Prize is shown and added to the local demo balance.
4. **Unity Rewarded Ad opens automatically** using `BP_Rewarded_Android`.
5. When the ad completes, **SPIN NOW** is unlocked again.
6. If the ad fails, the user gets **RETRY AD**; no new spin is unlocked.

There is intentionally **no fake Watch Ads card or custom ad creative**. The actual ad is supplied by Unity Ads.

## Unity configuration
- Game ID: `800390347`
- Rewarded placement: `BP_Rewarded_Android`
- Interstitial placement: `BP_Interstitial_Android` (not used in this flow)
- Banner placement: `BP_Banner_Android` (not used in this flow)
- Unity Ads SDK: `4.21.0`

## Build
Open the `SpinzaApp` folder in Android Studio and let Gradle sync. Then use **Build > Build APK(s)**.

This environment does not include the Gradle wrapper, so the ZIP is source code rather than a prebuilt APK.

## Important
This version stores virtual coins locally for demonstration. Do not use a client-side balance as the source of truth for real-money payouts. A production cash-conversion system needs a secure backend, transaction ledger, server-side reward validation, anti-fraud controls, and applicable legal/compliance review.

Unity Ads 4.21.0 documents the legacy static rewarded APIs used here as deprecated in favor of the newer instance-based API. This prototype keeps the integration compact; migrate to the newer API before production.

Set `TEST_MODE` to `false` only after you have completed Unity dashboard testing and implemented the required consent/privacy flow.
