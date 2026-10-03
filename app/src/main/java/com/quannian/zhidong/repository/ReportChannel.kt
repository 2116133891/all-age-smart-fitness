package com.quannian.zhidong.repository

import com.quannian.zhidong.model.TrainingReport

/**
 * 简易数据通道：把跟练页产出的 [TrainingReport] 传给报告页。
 *  单例，避免复杂化导航参数序列化；演示场景足够用。
 *  未来可替换为 ViewModel 共享 / SavedStateHandle。
 */
object ReportChannel {
    private var last: TrainingReport? = null

    fun put(report: TrainingReport) {
        last = report
    }

    fun take(): TrainingReport? = last
}
