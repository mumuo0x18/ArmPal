package cn.armpal.rehab.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * exercises.json 数据契约(2026-11-14 评审 P1:原来顶层 { version, exercises } 与单动作模型不匹配)。
 */
class ExerciseCatalogTest {

    @Test
    fun `解析目录包装与未知字段`() {
        val raw = """
            {
              "version": 1,
              "exercises": [
                {
                  "id": "seated_arm_raise",
                  "name": "坐姿抬臂",
                  "targetJoint": "WRIST_R",
                  "targetElevationDeg": 80.0,
                  "reps": 12,
                  "holdMs": 500,
                  "sets": 2,
                  "restSec": 30,
                  "description": "坐直,缓慢抬起右臂",
                  "futureField": {"ignored": true},
                  "templateTrajectory": [[0.0, -0.3, 0.0], [0.5, 0.1, 0.0]]
                }
              ]
            }
        """.trimIndent()

        val exercises = ExerciseRepository.decode(raw)
        assertEquals(1, exercises.size)
        val ex = exercises.first()
        assertEquals("seated_arm_raise", ex.id)
        assertEquals("WRIST_R", ex.targetJoint)
        assertEquals(80f, ex.targetElevationDeg, 1e-4f)
        assertEquals(12, ex.reps)
        assertEquals(2, ex.sets)
        assertEquals("坐直,缓慢抬起右臂", ex.description)
        assertEquals(2, ex.templateTrajectory.size)
    }

    @Test
    fun `空动作库 不崩溃`() {
        assertTrue(ExerciseRepository.decode("""{"version":1,"exercises":[]}""").isEmpty())
    }
}
