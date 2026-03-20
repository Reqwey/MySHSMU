package xyz.reqwey.myshsmu.ui.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import xyz.reqwey.myshsmu.MySHSMUUiState
import xyz.reqwey.myshsmu.MainViewModel
import xyz.reqwey.myshsmu.R

data class NavItem(val label: String, val iconId: Int, val iconCheckedId: Int)

@Composable
fun MainScreen(uiState: MySHSMUUiState, viewModel: MainViewModel) {
	var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

	val navItems = listOf(
		NavItem("课程", R.drawable.calendar_today_24px, R.drawable.calendar_today_24px_filled),
		NavItem("教室", R.drawable.school_24px, R.drawable.school_24px_filled),
		NavItem("成绩", R.drawable.kid_star_24px, R.drawable.kid_star_24px_filled),
		NavItem("设置", R.drawable.settings_24px, R.drawable.settings_24px_filled)
	)

	NavigationSuiteScaffold(
		navigationSuiteItems = {
			navItems.forEachIndexed { index, item ->
				item(
					selected = selectedItemIndex == index,
					onClick = { selectedItemIndex = index },
					icon = {
						Icon(
							painter = painterResource(id = if (selectedItemIndex == index) item.iconCheckedId else item.iconId),
							contentDescription = item.label
						)
					},
					label = { Text(item.label) }
				)
			}
		}
	) {
		// Content based on selection
		when (selectedItemIndex) {
			0 -> CurriculumScreen(
				uiState = uiState,
				onPageChanged = { date -> viewModel.onWeekPageChanged(date) },
				onCourseSelected = { course -> viewModel.onCourseSelected(course) }
			)

			1 -> CurriculumScreen(
				uiState = uiState,
				onPageChanged = { date -> viewModel.onWeekPageChanged(date) },
				onCourseSelected = { course -> viewModel.onCourseSelected(course) }
			)

			2 -> ScoreScreen(
				uiState = uiState,
				onYearSelected = { viewModel.fetchScoreData(year = it) },
				onSemesterSelected = { viewModel.fetchScoreData(semester = it) }
			)

			3 -> SettingsScreen(
				uiState = uiState,
				onFirstWeekStartDateChanged = { viewModel.updateFirstWeekStartDate(it) },
				onWeekCountChanged = { viewModel.updateWeekCount(it) },
				onCourseBlockHeightChanged = { viewModel.updateCourseBlockHeight(it) },
				onLogout = { viewModel.logout() },
				onRefresh = { viewModel.refreshAllData() },
				onCheckUpdate = { viewModel.checkForUpdates(manual = true) },
				onStartUpdate = { viewModel.startUpdateDownload() },
				onDismissUpdateDialog = { viewModel.dismissUpdateDialog() }
			)
		}
	}
}
