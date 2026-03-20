package xyz.reqwey.myshsmu

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.reqwey.myshsmu.service.ShsmuService

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

	@get:Rule
	val mainDispatcherRule = MainDispatcherRule()

	private lateinit var app: Application
	private lateinit var prefs: android.content.SharedPreferences

	@Before
	fun setUp() {
		app = ApplicationProvider.getApplicationContext()
		prefs = app.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
		prefs.edit().clear().commit()
	}

	@Test
	fun checkAutoLogin_shouldNotEnterMainBeforeFirstLoginSuccess() = runTest {
		prefs.edit()
			.putString("username", "20230001")
			.putString("password", "pwd")
			.putBoolean("has_logged_in_once", false)
			.commit()

		val viewModel = MainViewModel(app)
		advanceUntilIdle()

		assertFalse(viewModel.uiState.value.isLoggedIn)
		assertEquals("20230001", viewModel.uiState.value.savedUsername)
		assertEquals("pwd", viewModel.uiState.value.savedPassword)
	}

	@Test
	fun fetchScoreData_sessionExpired_shouldReloginAndRetryOnce() = runTest {
		val viewModel = MainViewModel(app)
		val service = mockk<ShsmuService>()
		viewModel.replaceShsmuServiceForTest(service)
		viewModel.setUiStateForTest(
			viewModel.uiState.value.copy(
				isLoggedIn = true,
				savedUsername = "20230001",
				savedPassword = "pwd"
			)
		)

		val scoreJson = JSONObject().apply {
			put("1", org.json.JSONArray().put("2025-2026"))
			put("2", org.json.JSONArray())
			put("4", "GPA: 4.0")
		}

		coEvery { service.getScore("2025-2026", 1) } throws IllegalStateException("SESSION_EXPIRED") andThen scoreJson
		every { service.isSessionExpiredError(any()) } answers {
			firstArg<Throwable>().message == "SESSION_EXPIRED"
		}
		coEvery { service.checkSessionValid() } returns true

		viewModel.fetchScoreData("2025-2026", 1)
		advanceUntilIdle()

		coVerify(exactly = 2) { service.getScore("2025-2026", 1) }
		assertTrue(viewModel.uiState.value.isLoggedIn)
		assertEquals("GPA: 4.0", viewModel.uiState.value.gpaInfo)
	}

	@Test
	fun fetchScoreData_reloginFailed_shouldFallbackToLoginScreen() = runTest {
		val viewModel = MainViewModel(app)
		val service = mockk<ShsmuService>()
		viewModel.replaceShsmuServiceForTest(service)
		viewModel.setUiStateForTest(
			viewModel.uiState.value.copy(
				isLoggedIn = true,
				savedUsername = "",
				savedPassword = ""
			)
		)

		coEvery { service.getScore("2025-2026", 1) } throws IllegalStateException("SESSION_EXPIRED")
		every { service.isSessionExpiredError(any()) } answers {
			firstArg<Throwable>().message == "SESSION_EXPIRED"
		}
		coEvery { service.checkSessionValid() } returns false

		viewModel.fetchScoreData("2025-2026", 1)
		advanceUntilIdle()

		assertFalse(viewModel.uiState.value.isLoggedIn)
	}

	private fun MainViewModel.replaceShsmuServiceForTest(service: ShsmuService) {
		val field = MainViewModel::class.java.getDeclaredField("shsmuService")
		field.isAccessible = true
		field.set(this, service)
	}

	@Suppress("UNCHECKED_CAST")
	private fun MainViewModel.setUiStateForTest(state: MySHSMUUiState) {
		val stateField = MainViewModel::class.java.getDeclaredField("_uiState")
		stateField.isAccessible = true
		val flow = stateField.get(this) as MutableStateFlow<MySHSMUUiState>
		flow.value = state
	}
}
