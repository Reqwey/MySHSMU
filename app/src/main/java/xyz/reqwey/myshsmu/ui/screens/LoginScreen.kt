package xyz.reqwey.myshsmu.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import xyz.reqwey.myshsmu.LoginUiState
import xyz.reqwey.myshsmu.MainViewModel

@Composable
fun LoginScreen(viewModel: MainViewModel, uiState: LoginUiState) {
	// 本地状态：如果已保存则使用保存值，否则使用默认值
	var username by remember(uiState.savedUsername) { mutableStateOf(uiState.savedUsername) }
	var password by remember(uiState.savedPassword) { mutableStateOf(uiState.savedPassword) }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Spacer(modifier = Modifier.height(40.dp))

		Text(
			text = "登录",
			style = MaterialTheme.typography.headlineMedium,
			color = MaterialTheme.colorScheme.primary
		)

		Spacer(modifier = Modifier.height(40.dp))

		// 用户名输入框
		OutlinedTextField(
			value = username,
			onValueChange = { username = it },
			label = { Text("学号") },
			modifier = Modifier.fillMaxWidth(),
			singleLine = true
		)

		Spacer(modifier = Modifier.height(10.dp))

		// 密码输入框
		OutlinedTextField(
			value = password,
			onValueChange = { password = it },
			label = { Text("密码") },
			visualTransformation = PasswordVisualTransformation(),
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
			modifier = Modifier.fillMaxWidth(),
			singleLine = true
		)

		Spacer(modifier = Modifier.height(40.dp))

		// 登录按钮
		Button(
			onClick = { viewModel.startLogin(username, password) },
			modifier = Modifier
				.fillMaxWidth()
				.height(50.dp),
			enabled = !uiState.isLoading
		) {
			if (uiState.isLoading) {
				CircularProgressIndicator(
					modifier = Modifier.size(24.dp),
					color = Color.White,
					strokeWidth = 2.dp
				)
				Spacer(modifier = Modifier.width(8.dp))
				Text("正在登录...")
			} else {
				Text("登录")
			}
		}
		Spacer(modifier = Modifier.weight(1f))
		Text(
			text = "酱紫办 by Reqwey",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
			modifier = Modifier.align(Alignment.CenterHorizontally)
		)
	}
}
