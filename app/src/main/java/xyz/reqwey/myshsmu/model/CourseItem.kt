package xyz.reqwey.myshsmu.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime

data class CourseItem(
	val title: String,
	var type: String,
	val location: String,
	val startTime: LocalDateTime,
	val endTime: LocalDateTime,
	val count: Int,
	val color: Color,
	val ids: CourseItemIds
)
