package com.spinearn.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var balanceText: TextView
    private lateinit var walletText: TextView
    private lateinit var spinButton: Button
    private lateinit var statusText: TextView
    private lateinit var wheel: SpinWheelView
    private lateinit var ads: UnityAdsManager

    private var coins = 0
    private var pendingReward = 0
    private var waitingForAd = false
    private var spinning = false

    private val prefs by lazy { getSharedPreferences("spinza", MODE_PRIVATE) }
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    private val background = Color.rgb(10, 9, 20)
    private val surface = Color.rgb(23, 20, 39)
    private val surface2 = Color.rgb(31, 27, 52)
    private val primary = Color.rgb(125, 75, 255)
    private val gold = Color.rgb(248, 197, 76)
    private val muted = Color.rgb(166, 160, 188)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background

        coins = prefs.getInt("coins", 0)
        buildUi()

        ads = UnityAdsManager(this, object : UnityAdsManager.Callback {
            override fun onAdReady() {
                runOnUiThread {
                    if (pendingReward > 0) {
                        statusText.text = "Rewarded ad is ready • Unlocking your reward next"
                    }
                }
            }

            override fun onAdStarted() {
                runOnUiThread {
                    statusText.text = "Rewarded ad playing…"
                    spinButton.isEnabled = false
                }
            }

            override fun onRewardEarned() {
                runOnUiThread {
                    // Credit the wheel result only after Unity confirms the rewarded event.
                    if (pendingReward > 0) {
                        addCoins(pendingReward)
                        val earned = pendingReward
                        pendingReward = 0
                        waitingForAd = false
                        spinButton.text = "SPIN NOW"
                        spinButton.isEnabled = true
                        statusText.text = "🎉 +${numberFormat.format(earned)} coins added • Spin again!"
                    }
                }
            }

            override fun onAdClosedWithoutReward() {
                runOnUiThread {
                    if (pendingReward > 0) {
                        waitingForAd = true
                        spinButton.text = "RETRY AD"
                        spinButton.isEnabled = true
                        statusText.text = "Ad wasn't completed. Watch it to unlock your ${numberFormat.format(pendingReward)}-coin reward."
                    }
                }
            }

            override fun onAdFailed(message: String) {
                runOnUiThread {
                    if (pendingReward > 0) {
                        waitingForAd = true
                        spinButton.text = "RETRY AD"
                        spinButton.isEnabled = true
                        statusText.text = "Ad unavailable. Tap RETRY AD to continue."
                    } else {
                        spinButton.isEnabled = true
                    }
                }
            }
        })
        ads.initialize()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
            setBackgroundColor(background)
        }

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "Spinza"
            textSize = 27f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tagline = TextView(this).apply {
            text = "Spin • Win • Earn"
            textSize = 12f
            setTextColor(muted)
        }
        brand.addView(title)
        brand.addView(tagline)
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))

        walletText = TextView(this).apply {
            text = "🪙 ${numberFormat.format(coins)}"
            textSize = 16f
            setTextColor(gold)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = rounded(surface2, 24f)
        }
        header.addView(walletText)
        root.addView(header)

        val subtitle = TextView(this).apply {
            text = "Your daily spin is waiting"
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(5), 0, dp(14))
        }
        root.addView(subtitle)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(surface, 28f)
        }

        val balanceLabel = TextView(this).apply {
            text = "TOTAL COINS"
            textSize = 11f
            setTextColor(muted)
            letterSpacing = 0.12f
        }
        card.addView(balanceLabel)

        balanceText = TextView(this).apply {
            text = numberFormat.format(coins)
            textSize = 34f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, dp(2), 0, dp(6))
        }
        card.addView(balanceText)

        wheel = SpinWheelView(this)
        card.addView(wheel, LinearLayout.LayoutParams(-1, dp(285)))

        spinButton = Button(this).apply {
            text = "SPIN NOW"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            background = rounded(primary, 18f)
            elevation = dp(3).toFloat()
            setOnClickListener {
                if (spinning) return@setOnClickListener
                if (waitingForAd) {
                    isEnabled = false
                    statusText.text = "Loading rewarded ad…"
                    ads.showRewarded()
                } else {
                    handleSpin()
                }
            }
        }
        card.addView(spinButton, LinearLayout.LayoutParams(-1, dp(56)).apply {
            topMargin = dp(8)
        })

        statusText = TextView(this).apply {
            text = "Spin the wheel to reveal your reward."
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(0, dp(10), 0, 0)
        }
        card.addView(statusText)

        val info = TextView(this).apply {
            text = "A rewarded ad appears after each spin. Your coins are credited only after the ad reward is confirmed."
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(130, 124, 151))
            setPadding(dp(10), dp(12), dp(10), 0)
        }
        card.addView(info)

        root.addView(card, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, 0)
        }
        listOf("⌂\nHome", "🪙\nWallet", "↺\nHistory").forEachIndexed { index, label ->
            val v = TextView(this).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(if (index == 0) Color.WHITE else muted)
                setPadding(0, dp(4), 0, dp(4))
            }
            nav.addView(v, LinearLayout.LayoutParams(0, dp(54), 1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun handleSpin() {
        if (spinning || waitingForAd) return

        spinning = true
        spinButton.isEnabled = false
        statusText.text = "Spinning…"

        wheel.spin { reward ->
            runOnUiThread {
                spinning = false
                pendingReward = reward
                waitingForAd = true
                spinButton.text = "LOADING AD…"
                spinButton.isEnabled = false
                statusText.text = "🎁 You won ${numberFormat.format(reward)} coins! Preparing your rewarded ad…"
                // The ad is intentionally shown automatically after the wheel result.
                ads.showRewarded()
            }
        }
    }

    private fun addCoins(amount: Int) {
        coins += amount
        prefs.edit().putInt("coins", coins).apply()
        balanceText.text = numberFormat.format(coins)
        walletText.text = "🪙 ${numberFormat.format(coins)}"
    }

    private fun rounded(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius.toInt()).toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
