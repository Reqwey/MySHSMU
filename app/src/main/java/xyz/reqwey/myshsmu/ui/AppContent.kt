package xyz.reqwey.myshsmu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.reqwey.myshsmu.LocalNotificationManager
import xyz.reqwey.myshsmu.MainViewModel
import xyz.reqwey.myshsmu.ui.components.LoadingNotification
import xyz.reqwey.myshsmu.ui.screens.LoginScreen
import xyz.reqwey.myshsmu.ui.screens.MainScreen

@Composable
fun AppContent(viewModel: MainViewModel) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val notificationManager = LocalNotificationManager.current

	LaunchedEffect(uiState.notificationState) {
		val state = uiState.notificationState
		if (state.isVisible) {
			notificationManager.show(state.message, state.status)
		} else {
			notificationManager.dismiss()
		}
	}

	Scaffold { paddingValues ->
		Box(
			modifier = Modifier
				.padding(paddingValues)
				.fillMaxSize()
		) {
			if (uiState.isLoggedIn) {
				MainScreen(uiState, viewModel)
			} else {
				LoginScreen(uiState, viewModel)
			}
			LoadingNotification(
				status = notificationManager.state.status,
				visible = notificationManager.state.isVisible,
				message = notificationManager.state.message,
				modifier = Modifier.align(Alignment.TopCenter)
			)
		}
	}
}
