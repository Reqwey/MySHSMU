package xyz.reqwey.myshsmu.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import xyz.reqwey.myshsmu.MySHSMUUiState
import xyz.reqwey.myshsmu.MainViewModel

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(uiState: MySHSMUUiState, viewModel: MainViewModel) {
	var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

	val navItems = listOf(
		NavItem("课程", Icons.Default.DateRange),
		NavItem("成绩", Icons.Default.Star),
		NavItem("设置", Icons.Default.Settings)
	)

	NavigationSuiteScaffold(
		navigationSuiteItems = {
			navItems.forEachIndexed { index, item ->
				item(
					selected = selectedItemIndex == index,
					onClick = { selectedItemIndex = index },
					icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
					label = { Text(item.label) }
				)
			}
		}
	) {
		// Content based on selection
		when (selectedItemIndex) {
			0 -> CurriculumScreen(
				uiState = uiState,
				onPageChanged = { date -> viewModel.onWeekPageChanged(date) })

			1 -> ScoreScreen(
				uiState = uiState,
				onYearSelected = { viewModel.fetchScoreData(year = it) },
				onSemesterSelected = { viewModel.fetchScoreData(semester = it) }
			)

			2 -> SettingsScreen(
				uiState = uiState,
				onFirstWeekStartDateChanged = { viewModel.updateFirstWeekStartDate(it) },
				onWeekCountChanged = { viewModel.updateWeekCount(it) },
				onLogout = { viewModel.logout() },
				onRefresh = { viewModel.refreshAllData() }
			)
		}
	}
}
