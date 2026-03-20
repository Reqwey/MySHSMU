package xyz.reqwey.myshsmu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.reqwey.myshsmu.model.GlobalNotificationState
import xyz.reqwey.myshsmu.ui.components.NotificationStatus

class NotificationManager {
	var state by mutableStateOf(GlobalNotificationState())
		private set

	private var dismissJob: Job? = null

	fun show(message: String, status: NotificationStatus, duration: Long = 3000) {
		dismissJob?.cancel() // 如果当前有正在倒计时的通知，先取消
		state = GlobalNotificationState(message, true, status)

		// 如果不是加载中状态，则定时自动关闭
		if (state.status != NotificationStatus.Loading) {
			dismissJob = CoroutineScope(Dispatchers.Main).launch {
				delay(duration)
				dismiss()
			}
		}
	}

	fun dismiss() {
		state = state.copy(isVisible = false)
	}
}

// 定义一个全局的 CompositionLocal
val LocalNotificationManager = staticCompositionLocalOf { NotificationManager() }