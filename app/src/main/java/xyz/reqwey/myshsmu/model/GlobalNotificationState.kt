package xyz.reqwey.myshsmu.model

import xyz.reqwey.myshsmu.ui.components.NotificationStatus

data class GlobalNotificationState(
	val message: String = "",
	val isVisible: Boolean = false,
	val status: NotificationStatus = NotificationStatus.Success
)