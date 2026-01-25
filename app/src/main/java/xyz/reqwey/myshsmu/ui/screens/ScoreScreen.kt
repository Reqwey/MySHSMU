package xyz.reqwey.myshsmu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
			.padding(horizontal = 16.dp)
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
					modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
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
				style = MaterialTheme.typography.bodySmall,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 8.dp)
			)
		}

		// Table Header
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 8.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			Column(
				modifier = Modifier.weight(3f),
			) {
				Text(
					"课程", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
					style = MaterialTheme.typography.bodySmall
				)
			}
			Column(
				modifier = Modifier.weight(1f),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					"学分", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
					style = MaterialTheme.typography.bodySmall
				)
			}
			Column(
				modifier = Modifier.weight(1.5f),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					"分数", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
					style = MaterialTheme.typography.bodySmall
				)
			}
			Column(
				modifier = Modifier.weight(1f),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					"评级", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
					style = MaterialTheme.typography.bodySmall
				)
			}
		}
		HorizontalDivider()

		// Score List
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
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
			.padding(vertical = 16.dp),
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Column(
			modifier = Modifier.weight(3f),
		) {
			Text(score.courseName, style = MaterialTheme.typography.bodyLarge)
		}

		Column(
			modifier = Modifier.weight(1f),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				score.credit.toString(),
				style = MaterialTheme.typography.bodyLarge,
				fontWeight = FontWeight.SemiBold,
			)
		}

		Column(
			modifier = Modifier.weight(1.5f),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = if (score.examSituation != "正常") score.examSituation else score.score.toString(),
				style = MaterialTheme.typography.bodyLarge,
				fontWeight = FontWeight.SemiBold,
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
		Column(
			modifier = Modifier.weight(1f),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = score.achievementGrade,
				style = MaterialTheme.typography.bodyLarge,
				fontWeight = FontWeight.SemiBold,
			)
		}
	}
}
