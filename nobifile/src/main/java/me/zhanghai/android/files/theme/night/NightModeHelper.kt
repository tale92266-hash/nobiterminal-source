/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.theme.night

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegateCompat
import me.zhanghai.android.files.app.AppActivity
import me.zhanghai.android.files.app.application
import me.zhanghai.android.files.settings.Settings
import me.zhanghai.android.files.util.SimpleActivityLifecycleCallbacks
import me.zhanghai.android.files.util.valueCompat

// We take over the activity creation when setting the default night mode from AppCompat so that:
// 1. We can recreate all activities upon change, instead of only started activities.
// 2. We can have custom handling of the change, instead of being forced to either recreate or
//    update resources configuration which is shared among activities.
object NightModeHelper {
    private val activities = mutableSetOf<AppCompatActivity>()

    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    // v29.2: same host-app guard as CustomThemeHelper — host
                    // activities (e.g. TermuxActivity, CordovaActivity) are
                    // NOT AppActivity subclasses and never call apply().
                    // Skipping them instead of throwing; the old check()
                    // would crash the host app on launch right after the
                    // CustomThemeHelper crash was fixed.
                    if (activity !is AppActivity) {
                        return
                    }
                    check(activity in activities) { "Activity must extend AppActivity: $activity" }
                }

                override fun onActivityDestroyed(activity: Activity) {
                    // v29.2: only AppCompatActivity instances are ever
                    // tracked; host activities (e.g. CordovaActivity
                    // extends plain Activity) must not crash this cast.
                    (activity as? AppCompatActivity)?.let { activities -= it }
                }
            })
    }

    fun apply(activity: AppCompatActivity) {
        activities += activity
        activity.delegate.localNightMode = nightMode
    }

    fun sync() {
        for (activity in activities) {
            val nightMode = nightMode
            if (activity is OnNightModeChangedListener) {
                if (getUiModeNight(activity.delegate.localNightMode, activity)
                    != getUiModeNight(nightMode, activity)) {
                    activity.onNightModeChangedFromHelper(nightMode)
                }
            } else {
                activity.delegate.localNightMode = nightMode
            }
        }
    }

    private val nightMode: Int
        get() = Settings.NIGHT_MODE.valueCompat.value

    /*
     * @see androidx.appcompat.app.AppCompatDelegateImpl#updateForNightMode(int, boolean)
     */
    private fun getUiModeNight(nightMode: Int, activity: AppCompatActivity): Int =
        when (AppCompatDelegateCompat.mapNightMode(activity.delegate, application, nightMode)) {
            AppCompatDelegate.MODE_NIGHT_YES -> Configuration.UI_MODE_NIGHT_YES
            AppCompatDelegate.MODE_NIGHT_NO -> Configuration.UI_MODE_NIGHT_NO
            else ->
                (activity.applicationContext.resources.configuration.uiMode
                    and Configuration.UI_MODE_NIGHT_MASK)
        }

    fun isInNightMode(activity: AppCompatActivity): Boolean =
        (getUiModeNight(activity.delegate.localNightMode, activity)
            == Configuration.UI_MODE_NIGHT_YES)

    interface OnNightModeChangedListener {
        fun onNightModeChangedFromHelper(nightMode: Int)
    }
}
