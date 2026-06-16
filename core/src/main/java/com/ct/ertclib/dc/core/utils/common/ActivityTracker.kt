package com.ct.ertclib.dc.core.utils.common

import android.app.Activity

object ActivityTracker {
    private var topActivityClassName: String? = null

    fun onActivityResumed(activity: Activity) {
        topActivityClassName = activity.javaClass.name
    }

    fun onActivityPaused(activity: Activity) {
        if (topActivityClassName == activity.javaClass.name) {
            topActivityClassName = null
        }
    }

    fun isTopActivity(className: String): Boolean {
        return topActivityClassName == className
    }

    fun getTopActivityClassName(): String? = topActivityClassName
}