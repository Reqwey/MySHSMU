package xyz.reqwey.myshsmu

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import xyz.reqwey.myshsmu.model.CourseItem
import xyz.reqwey.myshsmu.model.ScoreItem
import xyz.reqwey.myshsmu.network.NetworkModule
import xyz.reqwey.myshsmu.service.ShsmuService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// UI 状态数据类
data class LoginUiState(
	val isLoading: Boolean = false,
	val userMessage: String? = null,
	val curriculumJson: String? = null,
	val isLoggedIn: Boolean = false,
	val savedUsername: String = "",
	val savedPassword: String = "",
	val courseList: List<CourseItem> = emptyList(),
	val scoreYears: List<String> = emptyList(),
	val selectedYear: String? = null,
	val selectedSemester: Int = 1,
	val scoreList: List<ScoreItem> = emptyList(),
	val gpaInfo: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

	// 暴露给 Compose 观察的状态
	private val _uiState = MutableStateFlow(LoginUiState())
	val uiState = _uiState.asStateFlow()

	// 常量
	private val BASE_URL =
		"https://webvpn2.shsmu.edu.cn/https/77726476706e69737468656265737421f1e25594757e7b586d059ce29d51367b0014/cas/"
	private val LOGIN_URL =
		"https://webvpn2.shsmu.edu.cn/https/77726476706e69737468656265737421fae05288327e7b586d059ce29d51367b9aac/Home"
	private val HOME_URL =
		"https://webvpn2.shsmu.edu.cn/https/77726476706e69737468656265737421fae05288327e7b586d059ce29d51367b9aac/"
	private val PREF_NAME = "auth_prefs"
	private val PREF_CURRICULUM_RANGE_START = "curriculum_range_start"
	private val PREF_CURRICULUM_RANGE_END = "curriculum_range_end"
	private val PREF_CURRICULUM_JSON = "curriculum_json"

	private val shsmuService: ShsmuService
	private val prefs by lazy { application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }

	// Runtime cache for date range
	private var cachedStart: LocalDate? = null
	private var cachedEnd: LocalDate? = null

	init {
		// ViewModel 初始化时配置网络
		NetworkModule.init(application.applicationContext)
		shsmuService = ShsmuService(NetworkModule.client, BASE_URL, LOGIN_URL, HOME_URL)

		loadPersistentData()

		// 尝试自动登录
		checkAutoLogin()
	}

	private fun loadPersistentData() {
		val json = prefs.getString(PREF_CURRICULUM_JSON, null)
		val startStr = prefs.getString(PREF_CURRICULUM_RANGE_START, null)
		val endStr = prefs.getString(PREF_CURRICULUM_RANGE_END, null)

		if (json != null) {
			val list = parseJsonToCourseList(json)
			_uiState.value = _uiState.value.copy(
				curriculumJson = json,
				courseList = list
			)
		}
		if (startStr != null) cachedStart = LocalDate.parse(startStr)
		if (endStr != null) cachedEnd = LocalDate.parse(endStr)
	}

	private fun checkAutoLogin() {
		val user = prefs.getString("username", "") ?: ""
		val pass = prefs.getString("password", "") ?: ""

		if (user.isNotBlank() && pass.isNotBlank()) {
			_uiState.value = _uiState.value.copy(savedUsername = user, savedPassword = pass)
			startLogin(user, pass)
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
		_uiState.value = LoginUiState()
	}

	fun messageShown() {
		_uiState.value = _uiState.value.copy(userMessage = null)
	}

	fun startLogin(user: String, pwd: String) {
		if (user.isBlank() || pwd.isBlank()) {
			showMessage("错误：请将学号与密码填写完整")
			return
		}

		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true)

			try {
				// 读取公钥
				val pubKey = loadPubKey()
				if (pubKey == null) {
					showMessage("错误：无法找到加密公钥")
					return@launch
				}

				showMessage("尝试通过保存的 Cookie 登录...")
				if (shsmuService.checkSessionValid()) {
					showMessage("登录成功")
					// Even if session is valid, save credentials for future auto-login
					saveCredentials(user, pwd)
					_uiState.value = _uiState.value.copy(
						isLoggedIn = true,
						savedUsername = user,
						savedPassword = pwd
					)
					onWeekPageChanged(LocalDate.now())
					fetchScoreData()
				} else {
					showMessage("正在登录...")
					if (shsmuService.autoLogin(user, pwd, pubKey)) {
						showMessage("登录成功")
						saveCredentials(user, pwd)

						_uiState.value = _uiState.value.copy(
							isLoggedIn = true,
							savedUsername = user,
							savedPassword = pwd
						)
						onWeekPageChanged(LocalDate.now())
						fetchScoreData()
					} else {
						showMessage("用户名和密码不正确")
					}
				}
			} catch (e: Exception) {
				showMessage("错误：${e.localizedMessage}")
				e.printStackTrace()
			} finally {
				_uiState.value = _uiState.value.copy(isLoading = false)
			}
		}
	}

	private fun saveCredentials(user: String, pwd: String) {
		prefs.edit {
			putString("username", user)
			putString("password", pwd)
		}
	}

	fun refreshAllData() {
		val center = LocalDate.now()
		fetchWeekData(center.minusMonths(2), center.plusMonths(2))
		fetchScoreData()
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
			_uiState.value = _uiState.value.copy(isLoading = true)
			try {
				val startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE)
				val endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE)
				showMessage("正在加载课程...")
				val jsonObj = shsmuService.getCurriculum(startStr, endStr)
				mergeAndSaveCurriculumData(jsonObj, start, end)
			} catch (e: Exception) {
				val msg = "错误：${e.message}"
				showMessage(msg)
				e.printStackTrace()
			} finally {
				_uiState.value = _uiState.value.copy(isLoading = false)
			}
		}
	}

	fun fetchScoreData(year: String? = null, semester: Int? = null) {
		if (!_uiState.value.isLoggedIn) return

		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true)
			try {
				val targetYear = year ?: _uiState.value.selectedYear ?: "2025-2026"
				val targetSemester = semester ?: _uiState.value.selectedSemester

				val jsonObj = shsmuService.getScore(targetYear, targetSemester)

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

				val finalYear = if (targetYear.isEmpty() && years.isNotEmpty()) years.last() else targetYear

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

			} catch (e: Exception) {
				showMessage("获取成绩失败：${e.message}")
				e.printStackTrace()
			} finally {
				_uiState.value = _uiState.value.copy(isLoading = false)
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
			val oldJsonStr = prefs.getString(PREF_CURRICULUM_JSON, "{\"List\":[]}") ?: "{\"List\":[]}"
			val oldList = parseJsonToCourseList(oldJsonStr)
			val newList = parseJsonToCourseList(newData.toString())

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
				newJsonArray.put(obj)
			}
			newJsonRoot.put("List", newJsonArray)
			val finalJsonStr = newJsonRoot.toString()

			val newStart =
				if (cachedStart == null || fetchStart.isBefore(cachedStart)) fetchStart else cachedStart
			val newEnd = if (cachedEnd == null || fetchEnd.isAfter(cachedEnd)) fetchEnd else cachedEnd

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
		} catch (e: Exception) {
			e.printStackTrace()
			showMessage("数据存储失败：${e.message}")
		}
	}

	private fun parseJsonToCourseList(json: String): List<CourseItem> {
		val list = mutableListOf<CourseItem>()
		try {
			if (json.startsWith("[")) return emptyList()
			val root = JSONObject(json)
			val jsonArray = root.optJSONArray("List") ?: return emptyList()

			val colorPalette = listOf(
				Color(0xFFFFCDD2), Color(0xFFC8E6C9), Color(0xFFBBDEFB),
				Color(0xFFE1BEE7), Color(0xFFFFF9C4), Color(0xFFFFE0B2),
				Color(0xFFD1C4E9), Color(0xFFB2DFDB), Color(0xFFF8BBD0)
			)

			for (i in 0 until jsonArray.length()) {
				val obj = jsonArray.getJSONObject(i)
				val title = obj.optString("Curriculum", "Unknown")
				val type = obj.optString("CurriculumType", "Unknown")
				val count = obj.optInt("CourseCount", 0)
				val classroom = obj.optString("Classroom", "Unknown").replace("&nbsp;", "")
				val startStr = obj.optString("Start")
				val endStr = obj.optString("End")

				if (startStr.isNotEmpty() && endStr.isNotEmpty()) {
					val start = LocalDateTime.parse(startStr.replace(" ", "T"))
					val end = LocalDateTime.parse(endStr.replace(" ", "T"))
					val colorIndex = kotlin.math.abs(title.hashCode()) % colorPalette.size

					list.add(
						CourseItem(
							title = title,
							type = type,
							count = count,
							location = classroom,
							startTime = start,
							endTime = end,
							color = colorPalette[colorIndex]
						)
					)
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return list
	}

	private fun showMessage(msg: String) {
		_uiState.value = _uiState.value.copy(userMessage = msg)
	}

	private fun loadPubKey(): String? {
		return try {
			getApplication<Application>().assets.open("pubkey.pem").bufferedReader().use { it.readText() }
		} catch (e: Exception) {
			null
		}
	}
}