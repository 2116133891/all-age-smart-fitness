package com.quannian.zhidong.video

/**
 * 视频分析结果通道：把分析页产出的 [VideoAnalysisResult] + 视频 Uri 传给报告页/纠正页。
 *  单例（与 [com.quannian.zhidong.repository.ReportChannel] 同款），演示场景够用。
 */
object VideoAnalysisChannel {
    private var lastResult: VideoAnalysisResult? = null
    private var lastUri: android.net.Uri? = null
    private var lastExerciseKind: String? = null
    private var lastExerciseName: String? = null
    private var lastAgeId: String? = null

    fun put(
        result: VideoAnalysisResult,
        uri: android.net.Uri,
        exerciseKind: String,
        exerciseName: String,
        ageId: String
    ) {
        lastResult = result
        lastUri = uri
        lastExerciseKind = exerciseKind
        lastExerciseName = exerciseName
        lastAgeId = ageId
    }

    val result: VideoAnalysisResult? get() = lastResult
    val uri: android.net.Uri? get() = lastUri
    val exerciseKind: String? get() = lastExerciseKind
    val exerciseName: String? get() = lastExerciseName
    val ageId: String? get() = lastAgeId

    fun hasData(): Boolean = lastResult != null && lastUri != null

    /**
     * 分析**前**的待处理选择（运动 + 视频 Uri 字符串）。
     *  选视频页 [VideoPickScreen] 写入；分析页 [VideoAnalysisScreen] 读取。
     */
    data class Pending(
        val exerciseKind: String,
        val exerciseName: String,
        val ageId: String,
        val videoUri: String
    )

    private var pending: Pending? = null

    fun pending(kind: String, name: String, ageId: String, uriStr: String) {
        pending = Pending(kind, name, ageId, uriStr)
    }

    val pendingSelection: Pending? get() = pending

    /**
     * "数字教练针对纠正"标记（spec §十九）：从视频报告进入教学页时，
     * 教学页据此进入**纠错模式**——数字教练重新示范正确动作 + 主错误纠正示范。
     */
    private var correctionExerciseId: String? = null
    private var correctionError: String? = null

    fun markCorrection(exerciseId: String, errorType: String) {
        correctionExerciseId = exerciseId
        correctionError = errorType
    }

    /** 教学页读取纠正标记；读完由 [consumeCorrection] 清除。 */
    fun peekCorrection(): Pair<String?, String?> = correctionExerciseId to correctionError

    fun consumeCorrection() {
        correctionExerciseId = null
        correctionError = null
    }

    fun clear() {
        lastResult = null
        lastUri = null
        lastExerciseKind = null
        lastExerciseName = null
        lastAgeId = null
        pending = null
        correctionExerciseId = null
        correctionError = null
    }
}
