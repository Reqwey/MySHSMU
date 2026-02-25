package xyz.reqwey.myshsmu.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import xyz.reqwey.myshsmu.model.CourseItem
import xyz.reqwey.myshsmu.network.PersistentCookieJar
import xyz.reqwey.myshsmu.utils.CaptchaSolver
import xyz.reqwey.myshsmu.utils.RsaCrypto

class ShsmuService(
	private val client: OkHttpClient,
	private val cookieJar: PersistentCookieJar,
	private val baseUrl: String,
	private val loginUrl: String,
	private val homeUrl: String
) {
	suspend fun buildLoginForm(doc: Document, username: String, password: String): Pair<String, FormBody>? {
		try {
			// 提取表单信息
			val form = doc.selectFirst("form") ?: error("没有找到登录表单")

			// 提取 Action URL
			val action = form.attr("abs:action")
			val submitUrl = action.ifEmpty { loginUrl }

			// 提取所有输入字段 (包含隐藏字段)
			val inputFields = mutableMapOf<String, String>()
			form.select("input").forEach { input ->
				val name = input.attr("name")
				val type = input.attr("type")
				val value = input.attr("value")
				if (name.isNotEmpty() && type != "submit") {
					inputFields[name] = value
				}
			}

			// 查找并下载验证码
			val captchaImg = doc.selectFirst("img[src*=captcha]")
			val captchaUrl = captchaImg?.attr("abs:src") ?: "$baseUrl/captcha.jpg?vpn-1"
			Log.i("Login", "Downloading captcha from: $captchaUrl")

			val captchaBitmap = downloadImage(captchaUrl) ?: error("下载验证图片失败")

			// 识别验证码
			val captchaResult = CaptchaSolver.solve(captchaBitmap) ?: error("验证码识别失败")
			Log.i("Login", "Solved Captcha: $captchaResult")

			// 更新 map 中的关键字段
			inputFields["username"] = username
			inputFields["password"] = password
			inputFields["authcode"] = captchaResult

			val formBuilder = FormBody.Builder()
			// 7. 填入所有字段
			inputFields.forEach { (name, value) ->
				formBuilder.add(name, value)
			}

			return Pair(submitUrl, formBuilder.build())
		} catch (e: Exception) {
			Log.e("Login", e.toString())
			return null
		}
	}
	/**
	 * 全自动登录 (挂起函数，直接在 CoroutineScope 中调用)
	 */
	suspend fun autoLogin(username: String, password: String, publicKeyPem: String): Pair<Boolean, String> {
		return withContext(Dispatchers.IO) {
			try {
				val encryptedPwd = RsaCrypto.encryptPassword(password, publicKeyPem)
				var curLoginUrl = loginUrl
				for (i in 1..5) {
					cookieJar.clear()
					Log.i("Login", "Starting login process with url $curLoginUrl")

					// 获取登录页面 HTML以提取额外字段和验证码位置
					val loginPageHtml = getUrlContent(curLoginUrl) ?: error("未能下载登录页面")

					// 解析 HTML
					val doc = Jsoup.parse(loginPageHtml, curLoginUrl)
					val (submitUrl, form) = buildLoginForm(doc, username, encryptedPwd) ?: error("验证码AI处理失败，请再试一次")

					// 提交登录
					val postReq = Request.Builder()
						.url(submitUrl)
						.method("POST", form)
						.build()

					val loginResponse = client.newCall(postReq).execute()
					if (loginResponse.isSuccessful) {
						if (checkLoginSuccess(loginResponse.body.string())) {
							return@withContext Pair(true, "")
						}
						curLoginUrl = submitUrl
					} else {
						return@withContext Pair(false, "网络连接失败")
					}
				}

				return@withContext Pair(false, "用户名或密码错误")
			} catch (e: Exception) {
				Log.e("Login", "Auto login error", e)
				return@withContext Pair(false, "错误：${e.message}")
			}
		}
	}

	private fun getUrlContent(url: String): String? {
		val req = Request.Builder().url(url).get().build()
		return try {
			client.newCall(req).execute().use { resp ->
				if (resp.isSuccessful) {
					val content = resp.body.string()
					content
				} else {
					Log.e("Login", "Get content failed: HTTP ${resp.code}")
					null
				}
			}
		} catch (e: Exception) {
			Log.e("Login", "Get content error", e)
			null
		}
	}

	/**
	 * 尝试使用现有 Cookie 验证登录状态。
	 * 如果成功，说明不需要重新登录。
	 */
	suspend fun checkSessionValid(): Boolean {
		return withContext(Dispatchers.IO) {
			try {
				val req = Request.Builder().url(loginUrl).get().build()
				client.newCall(req).execute().use { resp ->
					checkLoginSuccess(resp.body.string())
				}
			} catch (e: Exception) {
				e.printStackTrace()
				false
			}
		}
	}

	private fun checkLoginSuccess(content: String): Boolean {
		return !content.contains("login_box")
	}

	private fun downloadImage(url: String): Bitmap? {
		val req = Request.Builder().url(url).get().build()
		return try {
			client.newCall(req).execute().use { resp ->
				if (resp.isSuccessful) {
					val bitmap = resp.body.byteStream().use { stream ->
						BitmapFactory.decodeStream(stream)
					}
					bitmap
				} else {
					Log.e("Login", "Image download failed: HTTP ${resp.code}")
					null
				}
			}
		} catch (e: Exception) {
			Log.e("Login", "Image download error", e)
			null
		}
	}

	suspend fun getCurriculum(start: String, end: String): JSONObject {
		return withContext(Dispatchers.IO) {
			val url =
				"${homeUrl}/Home/GetCurriculumTable?vpn-12-o2-jwstu.shsmu.edu.cn&Start=$start&End=$end"

			Log.i("Curriculum", "Requesting: $url")

			val req = Request.Builder()
				.url(url)
				.get()
				.header("Accept", "application/json, text/javascript, */*; q=0.01")
				.build()

			try {
				client.newCall(req).execute().use { resp ->
					if (resp.isSuccessful) {
						val body = resp.body.string()
						Log.d("Curriculum", "Response Body: $body")
						if (body.isBlank()) {
							throw Exception("Response body is empty")
						}
						JSONObject(body)
					} else {
						val errorBody = resp.body.string()
						Log.e("Curriculum", "HTTP ${resp.code}: $errorBody")
						throw Exception("HTTP ${resp.code}: $errorBody")
					}
				}
			} catch (e: Exception) {
				Log.e("Curriculum", "Network request failed", e)
				throw e
			}
		}
	}

	suspend fun getCourseDetail(course: CourseItem): JSONArray {
		return withContext(Dispatchers.IO) {
			val urlBuilder =
				"${homeUrl}/Home/GetCalendarTable?vpn-12-o2-jwstu.shsmu.edu.cn".toHttpUrlOrNull()
					?.newBuilder()
			urlBuilder?.apply {
				addQueryParameter("MCSID", course.ids.mcsId)
				addQueryParameter("CSID", course.ids.csId.toString())
				addQueryParameter("CurriculumID", course.ids.curriculumId.toString())
				addQueryParameter("XXKMID", course.ids.xxkmId)
				addQueryParameter("CurriculumType", course.type)
			}
			val url = urlBuilder?.build().toString()

			Log.i("CourseDetail", "Requesting: $url")
			val req = Request.Builder()
				.url(url)
				.get()
				.header("Accept", "application/json, text/javascript, */*; q=0.01")
				.build()
			try {
				client.newCall(req).execute().use { resp ->
					if (resp.isSuccessful) {
						val body = resp.body.string()
						Log.d("CourseDetail", "Response Body: $body")
						if (body.isBlank()) {
							throw Exception("Response body is empty")
						}
						JSONArray(body)
					} else {
						val errorBody = resp.body.string()
						Log.e("CourseDetail", "HTTP ${resp.code}: $errorBody")
						throw Exception("HTTP ${resp.code}: $errorBody")
					}
				}
			} catch (e: Exception) {
				Log.e("CourseDetail", "Network request failed", e)
				throw e
			}
		}
	}

	suspend fun getScore(grade: String, semester: Number): JSONObject {
		return withContext(Dispatchers.IO) {
			val url =
				"${homeUrl}/Score/GetStuYearScore?vpn-12-o2-jwstu.shsmu.edu.cn&Grade=$grade&Semester=$semester"

			Log.i("Score", "Requesting: $url")

			val req = Request.Builder()
				.url(url)
				.get()
				.header("Accept", "application/json, text/javascript, */*; q=0.01")
				.build()

			try {
				client.newCall(req).execute().use { resp ->
					if (resp.isSuccessful) {
						val body = resp.body.string()
						Log.d("Score", "Response Body: $body")
						if (body.isBlank()) {
							throw Exception("Response body is empty")
						}
						JSONObject(body)
					} else {
						val errorBody = resp.body.string()
						Log.e("Score", "HTTP ${resp.code}: $errorBody")
						throw Exception("HTTP ${resp.code}: $errorBody")
					}
				}
			} catch (e: Exception) {
				Log.e("Score", "Network request failed", e)
				throw e
			}
		}
	}
}