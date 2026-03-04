package xyz.reqwey.myshsmu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import xyz.reqwey.myshsmu.ui.AppContent
import xyz.reqwey.myshsmu.ui.theme.MySHSMUTheme
import xyz.reqwey.myshsmu.widget.CurriculumWidget

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

	override fun onResume() {
		super.onResume()
		lifecycleScope.launch {
			CurriculumWidget().updateAll(applicationContext)
		}
	}
}
