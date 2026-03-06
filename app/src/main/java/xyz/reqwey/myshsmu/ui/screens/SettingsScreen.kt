package xyz.reqwey.myshsmu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import xyz.reqwey.myshsmu.MySHSMUUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	uiState: MySHSMUUiState,
	onFirstWeekStartDateChanged: (String) -> Unit,
	onWeekCountChanged: (Int) -> Unit,
	onCourseBlockHeightChanged: (Int) -> Unit,
	onLogout: () -> Unit,
	onRefresh: () -> Unit
) {
	var showAboutDialog by remember { mutableStateOf(false) }
	var showDatePickerDialog by remember { mutableStateOf(false) }
	var showWeekCountDialog by remember { mutableStateOf(false) }
	var tempWeekCount by remember { mutableStateOf(uiState.weekCount.toString()) }
	var tempCourseBlockHeight by remember { mutableIntStateOf(uiState.courseBlockHeight) }
	val context = LocalContext.current
	val versionName = remember {
		try {
			val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
			packageInfo.versionName
		} catch (e: Exception) {
			"Unknown"
		}
	}

	if (showDatePickerDialog) {
		val parsedDate = try {
			LocalDate.parse(uiState.firstWeekStartDate)
		} catch (e: Exception) {
			LocalDate.now()
		}
		val initialMillis = parsedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
		val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

		DatePickerDialog(
			onDismissRequest = { showDatePickerDialog = false },
			confirmButton = {
				TextButton(onClick = {
					datePickerState.selectedDateMillis?.let { millis ->
						val date =
							Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
						onFirstWeekStartDateChanged(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
					}
					showDatePickerDialog = false
				}) {
					Text("确定")
				}
			},
			dismissButton = {
				TextButton(onClick = { showDatePickerDialog = false }) {
					Text("取消")
				}
			}
		) {
			DatePicker(state = datePickerState)
		}
	}

	if (showWeekCountDialog) {
		AlertDialog(
			onDismissRequest = { showWeekCountDialog = false },
			confirmButton = {
				TextButton(onClick = {
					tempWeekCount.toIntOrNull()?.let {
						if (it > 0) onWeekCountChanged(it)
					}
					showWeekCountDialog = false
				}) {
					Text("确定")
				}
			},
			dismissButton = {
				TextButton(onClick = { showWeekCountDialog = false }) {
					Text("取消")
				}
			},
			title = { Text("设置学期周数") },
			text = {
				OutlinedTextField(
					value = tempWeekCount,
					onValueChange = { tempWeekCount = it },
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
					singleLine = true
				)
			}
		)
	}

	if (showAboutDialog) {
		AlertDialog(
			onDismissRequest = { showAboutDialog = false },
			confirmButton = {
				TextButton(onClick = { showAboutDialog = false }) {
					Text("确定")
				}
			},
			title = { Text("关于酱紫办") },
			text = {
				Text("酱紫办 (MySHSMU) 是一款为上海交通大学医学院学生开发的教务辅助工具。\n\n当前版本: $versionName\n开发人员: Reqwey")
			},
			icon = {
				Icon(
					imageVector = Icons.Outlined.Info,
					contentDescription = null
				)
			}
		)
	}

	Column(
		modifier = Modifier
			.padding(16.dp)
			.verticalScroll(rememberScrollState()),
		verticalArrangement = Arrangement.spacedBy(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Spacer(modifier = Modifier.height(16.dp))

		// Avatar
		Icon(
			imageVector = Icons.Default.AccountCircle,
			contentDescription = "Avatar",
			modifier = Modifier.size(100.dp),
			tint = MaterialTheme.colorScheme.primary
		)

		// Student ID
		Text(
			text = uiState.savedUsername,
			style = MaterialTheme.typography.titleLarge
		)
		Button(
			onClick = onLogout,
			modifier = Modifier
				.fillMaxWidth()
				.height(50.dp),
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.error
			)
		) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.ExitToApp,
				contentDescription = "Exit",
				modifier = Modifier.size(16.dp)
			)

			Spacer(modifier = Modifier.width(8.dp))

			Text("登出")
		}

		OutlinedCard(
			shape = RoundedCornerShape(16.dp),
			border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
		) {
			Column(modifier = Modifier.padding(16.dp)) {
				Text(
					"课程设置",
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.primary,
					style = MaterialTheme.typography.bodySmall
				)

				Spacer(modifier = Modifier.height(16.dp))

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Text("第一周的第一天")
					TextButton(
						onClick = { showDatePickerDialog = true },
					) {
						Text(uiState.firstWeekStartDate ?: "未设置")
						Spacer(modifier = Modifier.width(8.dp))
						Icon(
							imageVector = Icons.Default.DateRange,
							contentDescription = "DateRange",
							modifier = Modifier.size(16.dp)
						)
					}
				}

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Text("学期周数")
					TextButton(
						onClick = { showWeekCountDialog = true },
					) {
						Text(
							if (uiState.weekCount > 0) {
								"${uiState.weekCount} 周"
							} else {
								"未设置"
							}
						)
						Spacer(modifier = Modifier.width(8.dp))
						Icon(
							imageVector = Icons.Default.Edit,
							contentDescription = "DateRange",
							modifier = Modifier.size(16.dp)
						)
					}
				}

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = "课程格高",
						style = MaterialTheme.typography.bodyLarge
					)
					Spacer(modifier = Modifier.width(16.dp))
					Slider(
						modifier = Modifier.fillMaxWidth(),
						value = tempCourseBlockHeight.toFloat(),
						onValueChange = { newValue ->
							tempCourseBlockHeight = newValue.toInt()
							onCourseBlockHeightChanged(tempCourseBlockHeight)
						},
						valueRange = 30f..60f,
						steps = 2
					)
				}

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Text("更新课程信息")
					FilledTonalIconButton(
						onClick = onRefresh,
					) {
						Icon(
							imageVector = Icons.Default.Refresh,
							contentDescription = "Refresh",
							modifier = Modifier.size(16.dp)
						)
					}
				}
			}
		}


		OutlinedButton(
			onClick = { showAboutDialog = true },
			modifier = Modifier
				.fillMaxWidth()
				.height(50.dp)
		) {
			Icon(
				imageVector = Icons.Outlined.Info,
				contentDescription = "Info",
				modifier = Modifier.size(16.dp)
			)

			Spacer(modifier = Modifier.width(8.dp))

			Text("关于")
		}
	}
}
