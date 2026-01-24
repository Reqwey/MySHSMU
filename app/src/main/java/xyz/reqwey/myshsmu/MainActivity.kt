package xyz.reqwey.myshsmu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import xyz.reqwey.myshsmu.ui.AppContent
import xyz.reqwey.myshsmu.ui.theme.MySHSMUTheme

class MainActivity : ComponentActivity() {
	private val viewModel: MainViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			MySHSMUTheme {
				AppContent(viewModel)
			}
		}
	}
}
