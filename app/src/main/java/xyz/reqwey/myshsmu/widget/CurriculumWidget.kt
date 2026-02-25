package xyz.reqwey.myshsmu.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import xyz.reqwey.myshsmu.MainActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import xyz.reqwey.myshsmu.utils.CurriculumUtils
import xyz.reqwey.myshsmu.model.CourseItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CurriculumWidget : GlanceAppWidget() {
	override suspend fun provideGlance(context: Context, id: GlanceId) {
		val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
		val json = prefs.getString("curriculum_json", null)

		val today = LocalDate.now()
		val now = LocalTime.now()

		val courses = if (json != null) {
			CurriculumUtils.parseJsonToCourseList(json).filter {
				val courseDate = it.startTime.toLocalDate()
				val courseEndTime = it.endTime.toLocalTime()
				courseDate.isEqual(today) && courseEndTime.isAfter(now)
			}.sortedBy { it.startTime }
		} else {
			emptyList()
		}

		provideContent {
			GlanceTheme {
				Box(
					modifier = GlanceModifier
						.fillMaxSize()
						.background(GlanceTheme.colors.background)
						.cornerRadius(16.dp)
						.clickable(actionStartActivity<MainActivity>())
						.padding(12.dp)
				) {
					if (courses.isEmpty()) {
						Box(
							modifier = GlanceModifier.fillMaxSize(),
							contentAlignment = Alignment.Center
						) {
							Text(
								text = "🎉今天没有课啦！",
								style = TextStyle(
									fontSize = 18.sp,
									fontWeight = FontWeight.Bold,
									color = GlanceTheme.colors.primary,
									textAlign = TextAlign.Center
								)
							)
						}
					} else {
						Column(modifier = GlanceModifier.fillMaxSize()) {
							Text(
								text = "今日剩余课程",
								style = TextStyle(
									fontSize = 16.sp,
									fontWeight = FontWeight.Bold,
									color = GlanceTheme.colors.onSurface
								),
							)

							LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
								items(courses) { course ->
									CourseWidgetItem(course)
								}
							}
						}
					}
				}
			}
		}
	}

	@SuppressLint("RestrictedApi")
	@androidx.compose.runtime.Composable
	private fun CourseWidgetItem(course: CourseItem) {
		val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
		val startTimeStr = course.startTime.format(timeFormatter)
		val endTimeStr = course.endTime.format(timeFormatter)

		Row(modifier = GlanceModifier.fillMaxWidth()
			.padding(top = 12.dp)
			.clickable(actionStartActivity<MainActivity>())) {
			Row(
				modifier = GlanceModifier
					.fillMaxWidth()
					.background(course.color)
					.cornerRadius(12.dp)
					.padding(8.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = "$startTimeStr\n$endTimeStr",
					style = TextStyle(fontSize = 12.sp, color = androidx.glance.unit.ColorProvider(Color.DarkGray))
				)

				Spacer(modifier = GlanceModifier.width(8.dp))

				Column {
					Text(
						text = course.title,
						style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = androidx.glance.unit.ColorProvider(Color.Black)),
						maxLines = 1
					)
					if (course.location.isNotEmpty()) {
						Text(
							text = "@${course.location}",
							style = TextStyle(fontSize = 12.sp, color = androidx.glance.unit.ColorProvider(Color.DarkGray)),
							maxLines = 1
						)
					}
				}
			}
		}
	}
}
