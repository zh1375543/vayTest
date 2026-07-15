package com.vaycore.finance.util

import android.app.Activity
import java.lang.ref.WeakReference
import java.util.*
import kotlin.system.exitProcess

object AppStackUtil {
    private var mActivityStack: Stack<WeakReference<Activity>>? = null

    /** Add activity to stack */
    fun addActivity(activity: Activity) {
        if (mActivityStack == null) {
            mActivityStack = Stack()
        }
        mActivityStack!!.add(WeakReference(activity))
    }

    /**
     * Check if weak references are released, clean up from stack if so
     */
    private fun checkWeakReference() {
        if (mActivityStack != null) {
            // use iterator for safe removal
            val it = mActivityStack!!.iterator()
            while (it.hasNext()) {
                val activityReference = it.next()
                val temp = activityReference.get()
                if (temp == null) {
                    it.remove()
                }
            }
        }
    }

    /** Get current activity (last pushed in stack) */
    private fun currentActivity(): Activity? {
        checkWeakReference()
        return if (mActivityStack != null && !mActivityStack!!.isEmpty()) {
            mActivityStack!!.lastElement().get()
        } else null
    }

    /** Finish current activity (last pushed in stack) */
    fun finishActivity() {
        val activity = currentActivity()
        activity?.let { finishActivity(it) }
    }

    /** Finish the specified activity */
    fun finishActivity(activity: Activity?) {
        if (activity != null && mActivityStack != null) {
            // use iterator for safe removal
            val it = mActivityStack!!.iterator()
            while (it.hasNext()) {
                val activityReference = it.next()
                val temp = activityReference.get()
                // clean up released activity
                if (temp == null) {
                    it.remove()
                    continue
                }
                if (temp === activity) {
                    it.remove()
                }
            }
            activity.finish()
        }
    }

    /** Finish all activities of the specified class */
    fun finishActivity(cls: Class<*>) {
        if (mActivityStack != null) {
            // use iterator for safe removal
            val it = mActivityStack!!.iterator()
            while (it.hasNext()) {
                val activityReference = it.next()
                val activity = activityReference.get()
                // clean up released activity
                if (activity == null) {
                    it.remove()
                    continue
                }
                if (activity.javaClass == cls) {
                    it.remove()
                    activity.finish()
                }
            }
        }
    }

    /** Finish all activities */
    fun finishAllActivity() {
        if (mActivityStack != null) {
            for (activityReference in mActivityStack!!) {
                val activity = activityReference.get()
                activity?.finish()
            }
            mActivityStack!!.clear()
        }
    }

    /** Exit the application */
    fun exitApp() {
        try {
            finishAllActivity()
            // exit JVM, release memory resources, 0 = normal exit
            exitProcess(0)
            // kill app process from system
//            Process.killProcess(Process.myPid())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}