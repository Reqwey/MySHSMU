package xyz.reqwey.myshsmu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.reqwey.myshsmu.ClassroomOption
import xyz.reqwey.myshsmu.ClassroomScheduleItem
import xyz.reqwey.myshsmu.MySHSMUUiState
import xyz.reqwey.myshsmu.constants.STANDARD_TIME_SLOTS
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScreen(
	uiState: MySHSMUUiState,
	onInit: () -> Unit,
	onCampusSelected: (String) -> Unit,
	onBuildingSelected: (String) -> Unit,
	onFloorSelected: (String) -> Unit,
	onClassroomSelected: (String) -> Unit,
	onDateChanged: (LocalDate) -> Unit
) {
	LaunchedEffect(Unit) {
		onInit()
	}

	val initialDate = remember { LocalDate.now() }
	val initialPage = Int.MAX_VALUE / 2
	val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }

	LaunchedEffect(pagerState.currentPage, uiState.selectedClassroomCode) {
		val selectedClassroom = uiState.selectedClassroomCode ?: return@LaunchedEffect
		if (selectedClassroom.isBlank()) return@LaunchedEffect
		val dayOffset = pagerState.currentPage - initialPage
		onDateChanged(initialDate.plusDays(dayOffset.toLong()))
	}

	val dayOffset = pagerState.currentPage - initialPage
	val currentDate = initialDate.plusDays(dayOffset.toLong())
	val dateLabel = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))

	Box(modifier = Modifier.fillMaxSize()) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 12.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				ClassroomSelector(
					modifier = Modifier.weight(1f),
					label = "校区",
					options = uiState.classroomCampusOptions,
					selectedCode = uiState.selectedCampusCode,
					enabled = true,
					onSelected = onCampusSelected
				)
				ClassroomSelector(
					modifier = Modifier.weight(1f),
					label = "楼栋",
					options = uiState.classroomBuildingOptions,
					selectedCode = uiState.selectedBuildingCode,
					enabled = !uiState.selectedCampusCode.isNullOrBlank(),
					onSelected = onBuildingSelected
				)
			}

			Spacer(modifier = Modifier.height(8.dp))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				ClassroomSelector(
					modifier = Modifier.weight(1f),
					label = "楼层",
					options = uiState.classroomFloorOptions,
					selectedCode = uiState.selectedFloorCode,
					enabled = !uiState.selectedBuildingCode.isNullOrBlank(),
					onSelected = onFloorSelected
				)
				ClassroomSelector(
					modifier = Modifier.weight(1f),
					label = "教室",
					options = uiState.classroomRoomOptions,
					selectedCode = uiState.selectedClassroomCode,
					enabled = !uiState.selectedFloorCode.isNullOrBlank(),
					onSelected = onClassroomSelected
				)
			}

			Spacer(modifier = Modifier.height(8.dp))

			Text(
				text = dateLabel,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary,
				modifier = Modifier
					.padding(vertical = 4.dp)
					.align(Alignment.CenterHorizontally)
			)

			HorizontalPager(
				state = pagerState,
				modifier = Modifier.weight(1f),
				userScrollEnabled = !(uiState.isClassroomOptionsLoading || uiState.isClassroomScheduleLoading)
			) {
				DayClassroomSchedule(
					scheduleItems = uiState.classroomScheduleList,
					selectedClassroomCode = uiState.selectedClassroomCode,
					courseBlockHeight = uiState.courseBlockHeight
				)
			}
		}

		if (uiState.isClassroomOptionsLoading || uiState.isClassroomScheduleLoading) {
			Box(
				modifier = Modifier
					.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				CircularProgressIndicator()
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassroomSelector(
	modifier: Modifier = Modifier,
	label: String,
	options: List<ClassroomOption>,
	selectedCode: String?,
	enabled: Boolean,
	onSelected: (String) -> Unit
) {
	var expanded by remember { mutableStateOf(false) }
	val selectedName = options.firstOrNull { it.code == selectedCode }?.name ?: "请选择"

	ExposedDropdownMenuBox(
		expanded = expanded,
		onExpandedChange = {
			if (enabled && options.isNotEmpty()) {
				expanded = !expanded
			}
		},
		modifier = modifier
	) {
		OutlinedTextField(
			value = selectedName,
			onValueChange = {},
			enabled = enabled,
			readOnly = true,
			singleLine = true,
			label = { Text(label) },
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
			modifier = Modifier
				.fillMaxWidth()
				.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = enabled)
		)

		ExposedDropdownMenu(
			expanded = expanded,
			onDismissRequest = { expanded = false }
		) {
			options.forEach { option ->
				DropdownMenuItem(
					text = { Text(option.name) },
					onClick = {
						onSelected(option.code)
						expanded = false
					}
				)
			}
		}
	}
}

@Composable
private fun DayClassroomSchedule(
	scheduleItems: List<ClassroomScheduleItem>,
	selectedClassroomCode: String?,
	courseBlockHeight: Int
) {
	val cellHeight = courseBlockHeight.dp
	val timeColWidth = 40.dp

	if (selectedClassroomCode.isNullOrBlank()) {
		Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
			Text("请先选择完整的校区、楼栋、楼层和教室")
		}
		return
	}

	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		val scheduleWidth = maxWidth - timeColWidth
		val scrollState = rememberScrollState()

		Row(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(scrollState)
		) {
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
							lineHeight = 12.sp,
							color = MaterialTheme.colorScheme.onSurface,
							textAlign = TextAlign.Center,
						)
					}
				}
			}

			Box(modifier = Modifier.width(scheduleWidth)) {
				Column(modifier = Modifier.fillMaxWidth()) {
					repeat(STANDARD_TIME_SLOTS.size) {
						Box(
							modifier = Modifier.size(scheduleWidth, cellHeight)
						)
					}
				}

				scheduleItems.forEach { item ->
					val courseStart = item.beginTime
					var startSlot = -1
					for (i in STANDARD_TIME_SLOTS.indices) {
						val slotStart = STANDARD_TIME_SLOTS[i].first
						if (!courseStart.isBefore(slotStart) && courseStart.isBefore(
								slotStart.plusMinutes(
									41
								)
							)
						) {
							startSlot = i
							break
						}
					}

					if (startSlot == -1) {
						startSlot =
							STANDARD_TIME_SLOTS.indexOfFirst { it.first.hour == courseStart.hour }
						if (startSlot == -1 && STANDARD_TIME_SLOTS.isNotEmpty()) startSlot = 0
					}

					if (startSlot == -1) return@forEach

					val matchedEndSlot = STANDARD_TIME_SLOTS.indexOfFirst { (slotStart, _) ->
						!item.endTime.isBefore(slotStart) && item.endTime.isBefore(
							slotStart.plusMinutes(
								41
							).plusNanos(1)
						)
					}
					val fallbackCount = max(
						1,
						((java.time.Duration.between(item.beginTime, item.endTime)
							.toMinutes() + 49) / 50).toInt()
					)
					val slotCount = if (matchedEndSlot >= startSlot) {
						matchedEndSlot - startSlot + 1
					} else {
						fallbackCount
					}
					val maxAvailableSlots = STANDARD_TIME_SLOTS.size - startSlot
					val finalSlotCount = slotCount.coerceIn(1, maxAvailableSlots)

					val yOffset = cellHeight * startSlot
					val blockHeight = cellHeight * finalSlotCount
					val cardColor = when {
						item.category.contains("自习") -> MaterialTheme.colorScheme.tertiaryContainer
						item.category.contains("考试") -> MaterialTheme.colorScheme.errorContainer
						else -> MaterialTheme.colorScheme.primaryContainer
					}

					Card(
						colors = CardDefaults.cardColors(containerColor = cardColor),
						modifier = Modifier
							.offset(x = 4.dp, y = yOffset)
							.width(scheduleWidth - 8.dp)
							.height(blockHeight - 2.dp),
						shape = RoundedCornerShape(8.dp),
						elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
					) {
						Box(
							modifier = Modifier
								.fillMaxSize()
								.padding(2.dp),
							contentAlignment = Alignment.Center
						) {
							Column(horizontalAlignment = Alignment.CenterHorizontally) {
								Text(
									text = item.courseName,
									fontWeight = FontWeight.Bold,
									fontSize = 12.sp,
									lineHeight = 14.sp
								)
								if (item.teacher.isNotBlank()) {
									Spacer(modifier = Modifier.height(5.dp))
									Text(
										text = item.teacher,
										fontSize = 10.sp,
										lineHeight = 12.sp,
										color = MaterialTheme.colorScheme.onSurfaceVariant
									)
								}
								if (item.className.isNotBlank()) {
									Spacer(modifier = Modifier.height(5.dp))
									Text(
										text = item.className,
										fontSize = 10.sp,
										lineHeight = 12.sp,
										color = MaterialTheme.colorScheme.onSurfaceVariant
									)
								}
								if (item.content.isNotBlank() && item.content != "-") {
									Spacer(modifier = Modifier.height(5.dp))
									Text(
										text = item.content,
										fontSize = 10.sp,
										lineHeight = 12.sp
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
