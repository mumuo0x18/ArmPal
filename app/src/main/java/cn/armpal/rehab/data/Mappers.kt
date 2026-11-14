package cn.armpal.rehab.data

import cn.armpal.rehab.session.SessionRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 会话记录 → Room 实体(data 层唯一的转换点)。 */
fun SessionRecord.toEntity(): TrainingSessionEntity = TrainingSessionEntity(
    date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    reps = reps,
    targetReps = targetReps,
    maxElevationDeg = maxElevationDeg,
    avgQuality = avgQuality,
    symmetryDeg = symmetryDeg,
    durationSec = durationSec,
    setsDone = setsDone,
    demoMode = demoMode,
    createdAtMs = createdAtMs,
)
