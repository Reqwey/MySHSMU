package xyz.reqwey.myshsmu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.reqwey.myshsmu.MainViewModel
import xyz.reqwey.myshsmu.ui.screens.LoginScreen
import xyz.reqwey.myshsmu.ui.screens.MainScreen

@Composable
fun AppContent(viewModel: MainViewModel) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(uiState.userMessage) {
		uiState.userMessage?.let { message ->
			snackbarHostState.showSnackbar(message)
			viewModel.messageShown()
		}
	}

	Scaffold { paddingValues ->
		Box(modifier = Modifier
			.padding(paddingValues)
			.fillMaxSize()) {
			if (uiState.isLoggedIn) {
				MainScreen(uiState, viewModel)
			} else {
				LoginScreen(viewModel, uiState)
			}
			SnackbarHost(
				hostState = snackbarHostState,
				modifier = Modifier.align(Alignment.TopCenter).padding(4.dp)
			)
		}
	}
}
