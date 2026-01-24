package xyz.reqwey.myshsmu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.reqwey.myshsmu.LoginUiState
import xyz.reqwey.myshsmu.model.ScoreItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreScreen(
	uiState: LoginUiState,
	onYearSelected: (String) -> Unit,
	onSemesterSelected: (Int) -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp)
	) {
		// Dropdowns Row
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			// Year Selector
			var yearExpanded by remember { mutableStateOf(false) }
			ExposedDropdownMenuBox(
				expanded = yearExpanded,
				onExpandedChange = { yearExpanded = !yearExpanded },
				modifier = Modifier.weight(1f)
			) {
				OutlinedTextField(
					value = uiState.selectedYear ?: "选择学年",
					onValueChange = {},
					readOnly = true,
					label = { Text("学年") },
					trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
					modifier = Modifier.menuAnchor()
				)
				ExposedDropdownMenu(
					expanded = yearExpanded,
					onDismissRequest = { yearExpanded = false }
				) {
					uiState.scoreYears.forEach { year ->
						DropdownMenuItem(
							text = { Text(year) },
							onClick = {
								onYearSelected(year)
								yearExpanded = false
							}
						)
					}
				}
			}

			// Semester Selector
			var semesterExpanded by remember { mutableStateOf(false) }
			val semesters = listOf(1, 2)
			ExposedDropdownMenuBox(
				expanded = semesterExpanded,
				onExpandedChange = { semesterExpanded = !semesterExpanded },
				modifier = Modifier.weight(1f)
			) {
				OutlinedTextField(
					value = "第 ${uiState.selectedSemester} 学期",
					onValueChange = {},
					readOnly = true,
					label = { Text("学期") },
					trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterExpanded) },
					modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
				)
				ExposedDropdownMenu(
					expanded = semesterExpanded,
					onDismissRequest = { semesterExpanded = false }
				) {
					semesters.forEach { sem ->
						DropdownMenuItem(
							text = { Text("第 $sem 学期") },
							onClick = {
								onSemesterSelected(sem)
								semesterExpanded = false
							}
						)
					}
				}
			}
		}

		Spacer(modifier = Modifier.height(16.dp))

		// GPA Info
		uiState.gpaInfo?.let {
			Text(
				text = it,
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 8.dp)
			)
		}

		// Table Header
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 8.dp),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Text("课程", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold)
			Text("学分", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
			Text("分数", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
			Text("评级", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
		}
		HorizontalDivider()

		// Score List
		LazyColumn(modifier = Modifier.fillMaxSize()) {
			items(uiState.scoreList) { score ->
				ScoreRow(score)
				HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), thickness = 0.5.dp)
			}
		}
	}
}

@Composable
fun ScoreRow(score: ScoreItem) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 12.dp),
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Column(modifier = Modifier.weight(3f)) {
			Text(score.courseName, style = MaterialTheme.typography.bodyMedium)
			if (score.examSituation != "正常") {
				Text(
					text = score.examSituation,
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.error
				)
			}
		}
		Text(
			text = score.credit.toString(),
			modifier = Modifier.weight(1f),
			style = MaterialTheme.typography.bodyMedium
		)
		Column(modifier = Modifier.weight(1.5f)) {
			Text(
				text = score.score.toString(),
				style = MaterialTheme.typography.bodyMedium,
				color = if (score.score < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
			)
			if (score.fScore > 0) {
				Text(
					text = "补: ${score.fScore}",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.secondary
				)
			}
		}
		Text(
			text = score.achievementGrade,
			modifier = Modifier.weight(1f),
			style = MaterialTheme.typography.bodyMedium
		)
	}
}
