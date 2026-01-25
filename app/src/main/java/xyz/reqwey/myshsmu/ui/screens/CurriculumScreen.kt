package xyz.reqwey.myshsmu.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.reqwey.myshsmu.model.CourseItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// Standard Time Slots Configuration
private val STANDARD_TIME_SLOTS = listOf(
	LocalTime.of(8, 0) to "08:00\n08:40",
	LocalTime.of(8, 50) to "08:50\n09:30",
	LocalTime.of(9, 40) to "09:40\n10:20",
	LocalTime.of(10, 30) to "10:30\n11:10",
	LocalTime.of(11, 20) to "11:20\n12:00",
	LocalTime.of(13, 30) to "13:30\n14:10",
	LocalTime.of(14, 20) to "14:20\n15:00",
	LocalTime.of(15, 10) to "15:10\n15:50",
	LocalTime.of(16, 0) to "16:00\n16:40",
	LocalTime.of(16, 50) to "16:50\n17:30",
	LocalTime.of(17, 40) to "17:40\n18:20",
	LocalTime.of(18, 30) to "18:30\n19:10",
	LocalTime.of(19, 20) to "19:20\n20:00",
	LocalTime.of(20, 10) to "20:10\n20:50"
)

@Composable
fun CurriculumScreen(
	courseList: List<CourseItem>,
	onPageChanged: (LocalDate) -> Unit
) {
	val initialDate = remember { LocalDate.now() }
	val initialPage = Int.MAX_VALUE / 2
	val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }

	LaunchedEffect(pagerState.currentPage) {
		val currentWeekOffset = pagerState.currentPage - initialPage
		val currentBaseDate = initialDate.plusWeeks(currentWeekOffset.toLong())
		// Start of that week (Monday)
		val weekStart = currentBaseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
		onPageChanged(weekStart)
	}


	// Title derived from current page
	val currentWeekOffset = pagerState.currentPage - initialPage
	val currentBaseDate = initialDate.plusWeeks(currentWeekOffset.toLong())
	val startOfCurrentWeek = currentBaseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
	val weekLabel =
		"${startOfCurrentWeek.year} 年第 ${startOfCurrentWeek.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)} 周"

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(horizontal = 8.dp)
	) {
		Text(
			text = weekLabel,
			style = MaterialTheme.typography.titleLarge,
			color = MaterialTheme.colorScheme.primary,
			modifier = Modifier.padding(bottom = 8.dp)
		)

		HorizontalPager(
			state = pagerState,
			modifier = Modifier.weight(1f)
		) { page ->
			val pageOffset = page - initialPage
			val pageDate = initialDate.plusWeeks(pageOffset.toLong())
			WeekSchedulePage(baseDate = pageDate, allCourses = courseList)
		}
	}
}


@Composable
fun WeekSchedulePage(baseDate: LocalDate, allCourses: List<CourseItem>) {
	val today = LocalDate.now()

	// Calculate Week Dates (Monday based)
	val startOfWeek = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
	val endOfWeek = startOfWeek.plusDays(6)
	val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }
	val currentMonthShort = startOfWeek.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

	// Filter courses for this week
	val weekCourses = allCourses.filter { course ->
		val courseDate = course.startTime.toLocalDate()
		!courseDate.isBefore(startOfWeek) && !courseDate.isAfter(endOfWeek)
	}

	BoxWithConstraints(
		modifier = Modifier.fillMaxSize()
	) {
		val totalWidth = maxWidth
		val timeColWidth = 40.dp
		val dayColWidth = (totalWidth - timeColWidth) / 7
		val headerHeight = 50.dp
		val cellHeight = 60.dp

		Column(modifier = Modifier.fillMaxSize()) {
			// --- Sticky Header Section ---
			Row(modifier = Modifier.fillMaxWidth()) {
				// Top-Left Header (Month)
				Box(
					modifier = Modifier
						.size(timeColWidth, headerHeight),
					contentAlignment = Alignment.Center
				) {
					Text(
						text = currentMonthShort,
						fontSize = 12.sp,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				// Day Headers
				Row(modifier = Modifier.weight(1f)) {
					weekDates.forEach { date ->
						val isToday = date.isEqual(today)
						val headerColor =
							if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

						val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
						val dayNumber = date.dayOfMonth.toString()

						Box(
							modifier = Modifier
								.size(dayColWidth, headerHeight),
							contentAlignment = Alignment.Center
						) {
							Column(horizontalAlignment = Alignment.CenterHorizontally) {
								Text(
									text = dayName,
									fontWeight = FontWeight.Bold,
									fontSize = 12.sp,
									color = headerColor
								)
								Text(
									text = dayNumber,
									fontSize = 10.sp,
									color = headerColor.copy(alpha = 0.8f)
								)
							}
						}
					}
				}
			}

			// --- Scrollable Body Section ---
			val scrollState = rememberScrollState()
			Row(
				modifier = Modifier
					.weight(1f) // Take remaining height
					.verticalScroll(scrollState)
			) {
				// 1. Time Column (Left)
				Column(modifier = Modifier.width(timeColWidth)) {
					STANDARD_TIME_SLOTS.forEach { (_, label) ->
						Box(
							modifier = Modifier
								.size(timeColWidth, cellHeight),
							contentAlignment = Alignment.Center
						) {
							Text(
								text = label,
								fontSize = 10.sp,
								textAlign = TextAlign.Center,
								lineHeight = 12.sp,
								color = MaterialTheme.colorScheme.onSurface
							)
						}
					}
				}

				// 2. Schedule Grid (Right)
				Box(modifier = Modifier.weight(1f)) {
					// Background Grid Placements
					Row(modifier = Modifier.fillMaxWidth()) {
						weekDates.forEach { _ ->
							Column(modifier = Modifier.width(dayColWidth)) {
								repeat(STANDARD_TIME_SLOTS.size) {
									Box(
										modifier = Modifier.size(dayColWidth, cellHeight)
									)
								}
							}
						}
					}

					// Overlay Courses
					weekCourses.forEach { course ->
						// Calculate Grid Position
						// Day Index (0-6)
						val dayIndex =
							ChronoUnit.DAYS.between(startOfWeek, course.startTime.toLocalDate()).toInt()

						// Find Start Slot
						val courseStart = course.startTime.toLocalTime()
						var startSlot = -1
						// Use a simple tolerance match or "contains" logic
						// Here we look for the last slot that starts before or at course time
						// Actually, strict match from SHSMU data usually aligns with slots.
						// We'll iterate to find the closest match.
						for (i in STANDARD_TIME_SLOTS.indices) {
							val slotStart = STANDARD_TIME_SLOTS[i].first
							// Allow slight variance or exact match
							if (!courseStart.isBefore(slotStart) && courseStart.isBefore(slotStart.plusMinutes(41))) { // 40 min class + margin
								startSlot = i
								break
							}
						}

						// If strict match fails, try finding the "earliest next slot" or just floor it
						if (startSlot == -1) {
							// Fallback: approximate based on hour? Or just skip/log error.
							// For robustness, let's just find closest slot start
							startSlot = STANDARD_TIME_SLOTS.indexOfFirst { it.first.hour == courseStart.hour }
							if (startSlot == -1 && STANDARD_TIME_SLOTS.isNotEmpty()) startSlot =
								0 // Valid fallback
						}

						if (startSlot != -1 && dayIndex in 0..6) {
							// Calculate duration in slots

							val xOffset = dayColWidth * dayIndex
							val yOffset = cellHeight * startSlot
							val height = cellHeight * course.count

							// Ensure it fits
							if (xOffset < (dayColWidth * 7)) {
								Card(
									colors = CardDefaults.cardColors(containerColor = course.color),
									modifier = Modifier
										.offset(x = xOffset, y = yOffset)
										.size(dayColWidth - 2.dp, height - 2.dp)
										.padding(1.dp),
									shape = RoundedCornerShape(8.dp),
									elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
								) {
									Box(
										modifier = Modifier
											.fillMaxSize()
											.padding(2.dp),
										contentAlignment = Alignment.Center
									) {
										Column(horizontalAlignment = Alignment.CenterHorizontally) {
											Text(
												text = "(${course.type.slice(0..1)}) ${course.title}",
												fontSize = 10.sp,
												textAlign = TextAlign.Center,
												color = Color.Black.copy(alpha = 0.8f),
												lineHeight = 11.sp,
											)
											if (course.location.isNotEmpty()) {
												Spacer(modifier = Modifier.height(10.dp))

												Text(
													text = "@${course.location}",
													fontSize = 8.sp,
													textAlign = TextAlign.Center,
													color = Color.Black.copy(alpha = 0.6f),
													lineHeight = 9.sp,
												)
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}




