package xyz.reqwey.myshsmu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class NotificationStatus {
	Loading,
	Success,
	Failed
}

@Composable
fun LoadingNotification(
	message: String,
	status: NotificationStatus,
	visible: Boolean,
	modifier: Modifier = Modifier
) {
	AnimatedVisibility(
		visible = visible,
		enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
		exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
		modifier = modifier
			.fillMaxWidth()
			.padding(16.dp)
	) {
		Surface(
			shape = RoundedCornerShape(12.dp),
			color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
			tonalElevation = 4.dp,
			shadowElevation = 8.dp,
			border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
		) {
			Row(
				modifier = Modifier
					.padding(horizontal = 16.dp, vertical = 12.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				when (status) {
					NotificationStatus.Loading ->
						CircularProgressIndicator(
							modifier = Modifier.size(20.dp),
							strokeWidth = 2.dp,
							color = MaterialTheme.colorScheme.primary
						)

					NotificationStatus.Success ->
						Icon(
							imageVector = Icons.Rounded.Done,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary
						)

					NotificationStatus.Failed ->
						Icon(
							imageVector = Icons.Rounded.Close,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.error
						)
				}

				Text(
					text = message,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface
				)
			}
		}
	}
}