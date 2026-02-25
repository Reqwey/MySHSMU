package xyz.reqwey.myshsmu.utils

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import xyz.reqwey.myshsmu.model.CourseItem
import xyz.reqwey.myshsmu.model.CourseItemIds
import java.time.LocalDateTime

object CurriculumUtils {

    val colorPalette = listOf(
        Color(0xFFFFCDD2), Color(0xFFC8E6C9), Color(0xFFBBDEFB),
        Color(0xFFE1BEE7), Color(0xFFFFF9C4), Color(0xFFFFE0B2),
        Color(0xFFD1C4E9), Color(0xFFB2DFDB), Color(0xFFF8BBD0)
    )

    fun parseJsonToCourseList(json: String): List<CourseItem> {
        val list = mutableListOf<CourseItem>()
        try {
            if (json.startsWith("[")) return emptyList()
            val root = JSONObject(json)
            val jsonArray = root.optJSONArray("List") ?: return emptyList()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("Curriculum", "Unknown")
                val type = obj.optString("CurriculumType", "Unknown")
                val count = obj.optInt("CourseCount", 0)
                val classroom = obj.optString("Classroom", "Unknown").replace("&nbsp;", "")
                val startStr = obj.optString("Start")
                val endStr = obj.optString("End")
                val ids = CourseItemIds(
                    mcsId = obj.optString("MCSID", ""),
                    csId = obj.optInt("CSID", 0),
                    curriculumId = obj.optInt("CurriculumID", 0),
                    xxkmId = obj.optString("XXKMID", "")
                )

                if (startStr.isNotEmpty() && endStr.isNotEmpty()) {
                    val start = LocalDateTime.parse(startStr.replace(" ", "T"))
                    val end = LocalDateTime.parse(endStr.replace(" ", "T"))
                    val colorIndex = kotlin.math.abs(title.hashCode()) % colorPalette.size

                    list.add(
                        CourseItem(
                            title = title,
                            type = type,
                            count = count,
                            location = classroom,
                            startTime = start,
                            endTime = end,
                            color = colorPalette[colorIndex],
                            ids = ids
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
