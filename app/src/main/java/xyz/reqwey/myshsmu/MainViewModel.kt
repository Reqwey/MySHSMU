package xyz.reqwey.myshsmu

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import xyz.reqwey.myshsmu.model.CourseDetail
import xyz.reqwey.myshsmu.model.CourseItem
import xyz.reqwey.myshsmu.model.ScoreItem
import xyz.reqwey.myshsmu.network.NetworkModule
import xyz.reqwey.myshsmu.service.ShsmuService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import xyz.reqwey.myshsmu.utils.CurriculumUtils
import androidx.glance.appwidget.updateAll
import xyz.reqwey.myshsmu.widget.CurriculumWidget
import androidx.core.net.toUri
import xyz.reqwey.myshsmu.model.GlobalNotificationState
import xyz.reqwey.myshsmu.ui.components.NotificationStatus

data class AppUpdateInfo(
	val version: String,
	val versionCode: Int,
	val downloadUrl: String,
	val updateLog: String,
	val forceUpdate: Boolean
)

// UI 状态数据类
data class MySHSMUUiState(
	val notificationState: GlobalNotificationState = GlobalNotificationState(),
	val curriculumJson: String? = null,
	val isLoggingIn: Boolean = false,
	val isLoggedIn: Boolean = false,
	val savedUsername: String = "",
	val savedPassword: String = "",
	val isCourseListLoading: Boolean = false,
	val courseList: List<CourseItem> = emptyList(),
	val courseDetail: CourseDetail? = null,
	val scoreYears: List<String> = emptyList(),
	val selectedYear: String? = null,
	val selectedSemester: Int = 1,
	val isScoreListLoading: Boolean = false,
	val scoreList: List<ScoreItem> = emptyList(),
	val gpaInfo: String? = null,
	val firstWeekStartDate: String? = null,
	val weekCount: Int = 0,
	val courseBlockHeight: Int = 60,
	val isCheckingUpdate: Boolean = false,
	val updateInfo: AppUpdateInfo? = null,
	val showUpdateDialog: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

	// 暴露给 Compose 观察的状态
	private val _uiState = MutableStateFlow(MySHSMUUiState())
	val uiState = _uiState.asStateFlow()

	// 常量
	private val BASE_URL =
		"https://webvpn2.shsmu.edu.cn/https/77726476706e69737468656265737421f1e25594757e7b586d059ce29d51367b0014/cas/"
	private val LOGIN_URL =
		"https://webvpn2.shsmu.edu.cn/https/77726476706e69737468656265737421f1e25594757e7b586d059ce29d51367b0014/cas/login?service=https%3a%2f%2fjwstu.shsmu.edu.cn%2fLogin%2fauthLogin"
	private val HOME_URL =
		"https://webvpn2.shsmu.edu.cn/https/77726476706e69737468656265737421fae05288327e7b586d059ce29d51367b9aac/"
	private val PREF_NAME = "auth_prefs"
	private val PREF_CURRICULUM_RANGE_START = "curriculum_range_start"
	private val PREF_CURRICULUM_RANGE_END = "curriculum_range_end"
	private val PREF_CURRICULUM_JSON = "curriculum_json"
	private val PREF_FIRST_WEEK_START_DATE = "first_week_start_date"
	private val PREF_WEEK_COUNT = "week_count"
	private val PREF_COURSE_BLOCK_HEIGHT = "course_block_height"
	private val PREF_HAS_LOGGED_IN_ONCE = "has_logged_in_once"
	private val UPDATE_HOST = "https://myshsmu.reqwey.xyz"
	private val UPDATE_JSON_URL = "$UPDATE_HOST/api/update.json"

	private val shsmuService: ShsmuService
	private val prefs by lazy { application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }

	// Runtime cache for date range
	private var cachedStart: LocalDate? = null
	private var cachedEnd: LocalDate? = null
	private val reLoginMutex = Mutex()

	private class AutoReLoginFailedException(message: String) : Exception(message)

	init {
		// ViewModel 初始化时配置网络
		NetworkModule.init(application.applicationContext)
		shsmuService = ShsmuService(
			NetworkModule.client,
			NetworkModule.cookieJar,
			BASE_URL,
			LOGIN_URL,
			HOME_URL
		)

		loadPersistentData()

		// 尝试自动登录
		checkAutoLogin()
		checkForUpdates()
	}

	private fun loadPersistentData() {
		val json = prefs.getString(PREF_CURRICULUM_JSON, null)
		val startStr = prefs.getString(PREF_CURRICULUM_RANGE_START, null)
		val endStr = prefs.getString(PREF_CURRICULUM_RANGE_END, null)
		val firstWeekStartDate = prefs.getString(PREF_FIRST_WEEK_START_DATE, null)
		val weekCount = prefs.getInt(PREF_WEEK_COUNT, 0)
		val courseBlockHeight = prefs.getInt(PREF_COURSE_BLOCK_HEIGHT, 60)

		if (json != null) {
			val list = CurriculumUtils.parseJsonToCourseList(json)
			_uiState.value = _uiState.value.copy(
				curriculumJson = json,
				courseList = list
			)
		}

		_uiState.value = _uiState.value.copy(
			firstWeekStartDate = firstWeekStartDate,
			weekCount = weekCount,
			courseBlockHeight = courseBlockHeight,
		)

		if (startStr != null) cachedStart = LocalDate.parse(startStr)
		if (endStr != null) cachedEnd = LocalDate.parse(endStr)
	}

	private fun checkAutoLogin() {
		val user = prefs.getString("username", "") ?: ""
		val pass = prefs.getString("password", "") ?: ""
		val hasLoggedInOnce = prefs.getBoolean(PREF_HAS_LOGGED_IN_ONCE, false)

		if (user.isNotBlank() && pass.isNotBlank()) {
			_uiState.value = _uiState.value.copy(
				savedUsername = user,
				savedPassword = pass,
				isLoggedIn = hasLoggedInOnce
			)

			if (hasLoggedInOnce) {
				onWeekPageChanged(LocalDate.now())
				fetchScoreData()
			}
		}
	}

	fun logout() {
		// 清除本地存储
		prefs.edit { clear() }
		cachedStart = null
		cachedEnd = null
		// 清除 Cookies
		try {
			NetworkModule.cookieJar.clear()
		} catch (e: Exception) {
			e.printStackTrace()
		}
		// 重置状态
		_uiState.value = MySHSMUUiState()
	}

	fun startLogin(user: String, pwd: String) {
		if (user.isBlank() || pwd.isBlank()) {
			updateNotification(NotificationStatus.Failed, "请将学号与密码填写完整")
			return
		}

		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoggingIn = true)

			try {
				// 读取公钥
				val pubKey = loadPubKey()
				if (pubKey == null) {
					updateNotification(NotificationStatus.Failed, "无法找到加密公钥")
					return@launch
				}
				if (shsmuService.checkSessionValid()) {
					updateNotification(NotificationStatus.Success, "登录成功")
					saveCredentials(user, pwd)
					markFirstLoginSuccess()
					_uiState.value = _uiState.value.copy(
						isLoggedIn = true,
						savedUsername = user,
						savedPassword = pwd
					)
					onWeekPageChanged(LocalDate.now())
					fetchScoreData()
				} else {
					updateNotification(NotificationStatus.Loading, "正在登录...")
					val (isSuccessful, message) = shsmuService.autoLogin(user, pwd, pubKey)
					if (isSuccessful) {
						updateNotification(NotificationStatus.Success, "登录成功")
						saveCredentials(user, pwd)
						markFirstLoginSuccess()

						_uiState.value = _uiState.value.copy(
							isLoggedIn = true,
							savedUsername = user,
							savedPassword = pwd
						)
						onWeekPageChanged(LocalDate.now())
						fetchScoreData()
					} else {
						updateNotification(NotificationStatus.Failed, message)
					}
				}
			} catch (e: Exception) {
				updateNotification(NotificationStatus.Failed, e.localizedMessage)
				e.printStackTrace()
			} finally {
				_uiState.value = _uiState.value.copy(isLoggingIn = false)
			}
		}
	}

	private fun markFirstLoginSuccess() {
		prefs.edit { putBoolean(PREF_HAS_LOGGED_IN_ONCE, true) }
	}

	private fun saveCredentials(user: String, pwd: String) {
		prefs.edit {
			putString("username", user)
			putString("password", pwd)
		}
	}

	fun updateFirstWeekStartDate(dateStr: String) {
		prefs.edit { putString(PREF_FIRST_WEEK_START_DATE, dateStr) }
		_uiState.value = _uiState.value.copy(firstWeekStartDate = dateStr)
	}

	fun updateWeekCount(count: Int) {
		prefs.edit { putInt(PREF_WEEK_COUNT, count) }
		_uiState.value = _uiState.value.copy(weekCount = count)
	}

	fun updateCourseBlockHeight(height: Int) {
		prefs.edit { putInt(PREF_COURSE_BLOCK_HEIGHT, height) }
		_uiState.value = _uiState.value.copy(courseBlockHeight = height)
	}

	fun refreshAllData() {
		val center = LocalDate.now()
		fetchWeekData(center.minusMonths(2), center.plusMonths(2))
		fetchScoreData()
	}

	fun checkForUpdates(manual: Boolean = false) {
		if (_uiState.value.isCheckingUpdate) return

		viewModelScope.launch {
			val currentVersionCode = getCurrentVersionCode()
			_uiState.value = _uiState.value.copy(isCheckingUpdate = true)
			if (manual) updateNotification(NotificationStatus.Loading, "正在检查更新...")

			try {
				val request = Request.Builder().url(UPDATE_JSON_URL).get().build()
				withContext(Dispatchers.IO) {
					NetworkModule.client.newCall(request).execute().use { response ->
						if (!response.isSuccessful) {
							if (manual) {
								updateNotification(
									NotificationStatus.Failed,
									"HTTP ${response.code}"
								)
							}
							return@use
						}

						val body = response.body.string().orEmpty()
						if (body.isBlank()) {
							if (manual) updateNotification(NotificationStatus.Failed, "响应为空")
							return@use
						}

						val updateJson = JSONObject(body)
						val remoteVersionCode = updateJson.optInt("versionCode", -1)

						if (remoteVersionCode <= currentVersionCode) {
							_uiState.value = _uiState.value.copy(
								updateInfo = null,
								showUpdateDialog = false
							)
							if (manual) updateNotification(
								NotificationStatus.Success,
								"当前已是最新版本"
							)
							return@use
						}

						val normalizedDownloadUrl = normalizeDownloadUrl(
							updateJson.optString("downloadUrl", "")
						)

						if (normalizedDownloadUrl == null) {
							if (manual) updateNotification(
								NotificationStatus.Failed,
								"下载地址无效"
							)
							return@use
						}

						val updateInfo = AppUpdateInfo(
							version = updateJson.optString("version", remoteVersionCode.toString()),
							versionCode = remoteVersionCode,
							downloadUrl = normalizedDownloadUrl,
							updateLog = updateJson.optString("updateLog", ""),
							forceUpdate = updateJson.optBoolean("forceUpdate", false)
						)

						_uiState.value = _uiState.value.copy(
							updateInfo = updateInfo,
							showUpdateDialog = true
						)

						if (manual) updateNotification(
							NotificationStatus.Success,
							"发现新版本 v${updateInfo.version}"
						)
					}
				}
			} catch (e: Exception) {
				Log.e("CheckForUpdates", "Network request failed", e)
				if (manual) updateNotification(
					NotificationStatus.Failed,
					e.localizedMessage ?: "未知错误"
				)
			} finally {
				_uiState.value = _uiState.value.copy(isCheckingUpdate = false)
			}
		}
	}

	fun dismissUpdateDialog() {
		val current = _uiState.value
		if (current.updateInfo?.forceUpdate == true) return
		_uiState.value = current.copy(showUpdateDialog = false)
	}

	fun startUpdateDownload() {
		val updateInfo = _uiState.value.updateInfo ?: return
		try {
			val intent = Intent(Intent.ACTION_VIEW, updateInfo.downloadUrl.toUri()).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			}
			getApplication<Application>().startActivity(intent)
		} catch (e: Exception) {
			updateNotification(NotificationStatus.Failed, "无法打开下载链接: ${e.localizedMessage}")
		}
	}

	fun onWeekPageChanged(date: LocalDate) {
		if (cachedStart == null || cachedEnd == null) {
			fetchWeekData(date.minusWeeks(2), date.plusWeeks(2))
			return
		}

		val threshold = 1L
		if (date.isBefore(cachedStart!!.plusWeeks(threshold))) {
			fetchWeekData(date.minusWeeks(4), cachedStart!!)
		} else if (date.isAfter(cachedEnd!!.minusWeeks(threshold))) {
			fetchWeekData(cachedEnd!!, date.plusWeeks(4))
		}
	}

	private fun fetchWeekData(start: LocalDate, end: LocalDate) {
		if (!_uiState.value.isLoggedIn) return

		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isCourseListLoading = true)
			try {
				val startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE)
				val endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE)
				updateNotification(NotificationStatus.Loading, "正在加载课程...")
				val jsonObj = executeWithAutoReLogin {
					shsmuService.getCurriculum(startStr, endStr)
				}
				mergeAndSaveCurriculumData(jsonObj, start, end)
				updateNotification(NotificationStatus.Success, "加载成功")
			} catch (e: AutoReLoginFailedException) {
				handleSessionExpired(e.localizedMessage)
			} catch (e: Exception) {
				updateNotification(NotificationStatus.Failed, e.localizedMessage)
				e.printStackTrace()
			} finally {
				_uiState.value = _uiState.value.copy(isCourseListLoading = false)
			}
		}
	}

	fun onCourseSelected(course: CourseItem) {
		viewModelScope.launch {
			try {
				_uiState.value = _uiState.value.copy(courseDetail = null)
				val jsonArr = executeWithAutoReLogin {
					shsmuService.getCourseDetail(course)
				}
				_uiState.value =
					_uiState.value.copy(courseDetail = parseCourseDetailObject(jsonArr))
			} catch (e: AutoReLoginFailedException) {
				handleSessionExpired(e.localizedMessage)
			} catch (e: Exception) {
				updateNotification(NotificationStatus.Failed, e.localizedMessage)
				e.printStackTrace()
			}
		}
	}

	private fun parseCourseDetailObject(obj: JSONArray): CourseDetail? {
		try {
			val detailObj = obj.optJSONObject(0)
			if (detailObj != null) {
				return CourseDetail(
					name = detailObj.optString("CourseName", ""),
					college = detailObj.optString("College", ""),
					teacher = "${detailObj.optString("Teacher", "")} ${
						detailObj.optString(
							"Title",
							""
						)
					}",
					content = detailObj.optString("Content", ""),
					classes = detailObj.optString("ClassCode", ""),
					location = detailObj.optString("Classroom_Name", "")
				)
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

	fun fetchScoreData(year: String? = null, semester: Int? = null) {
		if (!_uiState.value.isLoggedIn) return

		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isScoreListLoading = true)
			try {
				val targetYear = year ?: _uiState.value.selectedYear ?: "2025-2026"
				val targetSemester = semester ?: _uiState.value.selectedSemester

				val jsonObj = executeWithAutoReLogin {
					shsmuService.getScore(targetYear, targetSemester)
				}

				// Parse years
				val yearsArray = jsonObj.optJSONArray("1")
				val years = mutableListOf<String>()
				if (yearsArray != null) {
					for (i in 0 until yearsArray.length()) {
						years.add(yearsArray.getString(i))
					}
				}

				// Parse scores
				val scoreItems = mutableListOf<ScoreItem>()
				val scoreData = jsonObj.optJSONArray("2")
				if (scoreData != null) {
					for (i in 0 until scoreData.length()) {
						val semArray = scoreData.optJSONArray(i) ?: continue
						for (j in 0 until semArray.length()) {
							val obj = semArray.getJSONObject(j)
							val sem = obj.optInt("Semester")
							if (targetSemester == sem) {
								scoreItems.add(parseScoreObject(obj))
							}
						}
					}
				}

				val gpaInfo = jsonObj.optString("4", "")

				val finalYear =
					if (targetYear.isEmpty() && years.isNotEmpty()) years.last() else targetYear

				_uiState.value = _uiState.value.copy(
					scoreYears = years,
					selectedYear = finalYear.ifEmpty { null },
					selectedSemester = targetSemester,
					scoreList = scoreItems,
					gpaInfo = gpaInfo
				)

				// If it was initial fetch and year was empty, fetch again with the correct year
				if (targetYear.isEmpty() && finalYear.isNotEmpty()) {
					fetchScoreData(finalYear, targetSemester)
				}
			} catch (e: AutoReLoginFailedException) {
				handleSessionExpired(e.localizedMessage)
			} catch (e: Exception) {
				updateNotification(NotificationStatus.Failed, e.localizedMessage)
				e.printStackTrace()
			} finally {
				_uiState.value = _uiState.value.copy(isScoreListLoading = false)
			}
		}
	}

	private fun parseScoreObject(obj: JSONObject): ScoreItem {
		return ScoreItem(
			courseName = obj.optString("CurriculumName", "Unknown"),
			score = obj.optDouble("Score", 0.0),
			fScore = obj.optDouble("FScore", 0.0),
			achievementGrade = obj.optString("AchievementGrade", ""),
			credit = obj.optDouble("Credit", 0.0),
			examSituation = obj.optString("ExaminationSituationStr", ""),
			semester = obj.optInt("Semester", 0)
		)
	}

	private fun mergeAndSaveCurriculumData(
		newData: JSONObject,
		fetchStart: LocalDate,
		fetchEnd: LocalDate
	) {
		try {
			val oldJsonStr =
				prefs.getString(PREF_CURRICULUM_JSON, "{\"List\":[]}") ?: "{\"List\":[]}"
			val oldList = CurriculumUtils.parseJsonToCourseList(oldJsonStr)
			val newList = CurriculumUtils.parseJsonToCourseList(newData.toString())

			val mergedMap = oldList.associateBy { "${it.title}_${it.startTime}" }.toMutableMap()
			newList.forEach { mergedMap["${it.title}_${it.startTime}"] = it }

			val finalCourseList = mergedMap.values.toList().sortedBy { it.startTime }

			val newJsonRoot = JSONObject()
			val newJsonArray = JSONArray()
			finalCourseList.forEach { course ->
				val obj = JSONObject()
				obj.put("Curriculum", course.title)
				obj.put("CurriculumType", course.type)
				obj.put("CourseCount", course.count)
				obj.put("Classroom", course.location)
				obj.put("Start", course.startTime.toString())
				obj.put("End", course.endTime.toString())
				obj.put("MCSID", course.ids.mcsId)
				obj.put("CSID", course.ids.csId)
				obj.put("CurriculumID", course.ids.curriculumId)
				obj.put("XXKMID", course.ids.xxkmId)
				newJsonArray.put(obj)
			}
			newJsonRoot.put("List", newJsonArray)
			val finalJsonStr = newJsonRoot.toString()

			val newStart =
				if (cachedStart == null || fetchStart.isBefore(cachedStart)) fetchStart else cachedStart
			val newEnd =
				if (cachedEnd == null || fetchEnd.isAfter(cachedEnd)) fetchEnd else cachedEnd

			cachedStart = newStart
			cachedEnd = newEnd

			prefs.edit {
				putString(PREF_CURRICULUM_JSON, finalJsonStr)
				putString(PREF_CURRICULUM_RANGE_START, newStart?.toString())
				putString(PREF_CURRICULUM_RANGE_END, newEnd?.toString())
			}

			_uiState.value = _uiState.value.copy(
				courseList = finalCourseList,
				curriculumJson = finalJsonStr
			)

			viewModelScope.launch {
				try {
					CurriculumWidget().updateAll(getApplication())
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			updateNotification(NotificationStatus.Failed, e.localizedMessage)
		}
	}

	private suspend fun <T> executeWithAutoReLogin(block: suspend () -> T): T {
		return try {
			block()
		} catch (e: Exception) {
			if (!shsmuService.isSessionExpiredError(e)) throw e

			val reLoginSuccess = attemptBackgroundReLogin()
			if (!reLoginSuccess) {
				throw AutoReLoginFailedException("登录状态已失效，请重新登录")
			}

			block()
		}
	}

	private suspend fun attemptBackgroundReLogin(): Boolean {
		return reLoginMutex.withLock {
			if (shsmuService.checkSessionValid()) {
				_uiState.value = _uiState.value.copy(isLoggedIn = true)
				return@withLock true
			}

			val user = _uiState.value.savedUsername.ifBlank { prefs.getString("username", "") ?: "" }
			val pass = _uiState.value.savedPassword.ifBlank { prefs.getString("password", "") ?: "" }
			if (user.isBlank() || pass.isBlank()) return@withLock false

			val pubKey = loadPubKey() ?: return@withLock false
			val (isSuccessful, _) = shsmuService.autoLogin(user, pass, pubKey)
			if (isSuccessful) {
				saveCredentials(user, pass)
				markFirstLoginSuccess()
				_uiState.value = _uiState.value.copy(
					isLoggedIn = true,
					savedUsername = user,
					savedPassword = pass
				)
			}

			isSuccessful
		}
	}

	private fun handleSessionExpired(message: String?) {
		updateNotification(NotificationStatus.Failed, message ?: "登录状态已失效，请重新登录")
		_uiState.value = _uiState.value.copy(isLoggedIn = false, courseDetail = null)
	}


	private fun updateNotification(status: NotificationStatus, msg: String) {
		_uiState.value =
			_uiState.value.copy(notificationState = GlobalNotificationState(msg, true, status))
	}

	private fun normalizeDownloadUrl(rawUrl: String): String? {
		val trimmed = rawUrl.trim()
		if (trimmed.isBlank()) return null

		return when {
			trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
			trimmed.startsWith("/") -> "$UPDATE_HOST$trimmed"
			else -> "$UPDATE_HOST/$trimmed"
		}
	}

	private fun getCurrentVersionCode(): Int {
		return try {
			val packageInfo = getApplication<Application>().packageManager
				.getPackageInfo(getApplication<Application>().packageName, 0)
			packageInfo.longVersionCode.toInt()
		} catch (_: Throwable) {
			0
		}
	}

	private fun loadPubKey(): String? {
		return try {
			getApplication<Application>().assets.open("pubkey.pem").bufferedReader()
				.use { it.readText() }
		} catch (e: Exception) {
			null
		}
	}
}