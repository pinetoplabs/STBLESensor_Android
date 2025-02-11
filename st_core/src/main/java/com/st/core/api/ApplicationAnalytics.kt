package com.st.core.api

import android.app.Activity
import android.app.Application
import android.content.Context

interface ApplicationAnalyticsService {
    fun createAnalytics(
        etnaApplication: ApplicationNameEtna,
        application: Application,
        activity: Activity
    )

    enum class ApplicationNameEtna {
        STBLESensorDev,
        STBLESensorRel,
        STAssetTrackingDev,
        STAssetTrackingRel
    }

    fun reportApplicationAnalytics(context: Context)

    fun reportNodeAnalytics(
        nodeName: String,
        nodeType: String,
        fwVersion: String,
        FwFullName: String
    )

    fun reportProfile(profile: String)

    fun reportLevel(level: String)

    fun startDemoAnalytics(demoName: String)

    fun stopDemoAnalytics()

    fun flowExampleAppAnalytics(flowName: String)

    fun flowExpertAppAnalytics(flowName: String)

    fun flowExpertAppInputSensorAnalytics(id: String, model: String, odr: Double?)

    fun flowExpertAppFunctionAnalytics(id: String, desc: String)

    fun flowExpertAppOutputAnalytics(id: String, desc: String)

}