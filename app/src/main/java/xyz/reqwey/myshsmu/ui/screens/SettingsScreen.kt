package xyz.reqwey.myshsmu.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(username: String, onLogout: () -> Unit, onRefresh: () -> Unit) {
	var showAboutDialog by remember { mutableStateOf(false) }
	val context = LocalContext.current
	val versionName = remember {
		try {
			val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
			packageInfo.versionName
		} catch (e: Exception) {
			"Unknown"
		}
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
			.fillMaxSize()
			.padding(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Spacer(modifier = Modifier.height(40.dp))

		// Avatar
		Icon(
			imageVector = Icons.Default.AccountCircle,
			contentDescription = "Avatar",
			modifier = Modifier.size(100.dp),
			tint = MaterialTheme.colorScheme.primary
		)

		Spacer(modifier = Modifier.height(16.dp))

		// Student ID
		Text(
			text = username,
			style = MaterialTheme.typography.titleLarge
		)

		Spacer(modifier = Modifier.height(40.dp))

		// Menu List

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

		Spacer(modifier = Modifier.height(16.dp))

		OutlinedButton(
			onClick = onRefresh,
			modifier = Modifier
				.fillMaxWidth()
				.height(50.dp)
		) {
			Icon(
				imageVector = Icons.Default.Refresh,
				contentDescription = "Refresh",
				modifier = Modifier.size(16.dp)
			)

			Spacer(modifier = Modifier.width(8.dp))

			Text("更新课程信息")
		}

		Spacer(modifier = Modifier.height(16.dp))
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
