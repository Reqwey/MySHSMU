package xyz.reqwey.myshsmu.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import xyz.reqwey.myshsmu.utils.CaptchaSolver
import xyz.reqwey.myshsmu.utils.RsaCrypto

class ShsmuService(
	private val client: OkHttpClient,
	private val baseUrl: String,
	private val loginUrl: String,
	private val homeUrl: String
) {
	/**
	 * 全自动登录 (挂起函数，直接在 CoroutineScope 中调用)
	 */
	suspend fun autoLogin(username: String, password: String, publicKeyPem: String): Boolean {
		return withContext(Dispatchers.IO) {
			try {
				Log.i("Login", "Starting login process with url $loginUrl")

				// 1. 获取登录页面 HTML以提取额外字段和验证码位置
				val loginPageHtml = getUrlContent(loginUrl)
				if (loginPageHtml == null) {
					Log.e("Login", "Failed to get login page content")
					return@withContext false
				}

				// 2. 解析 HTML
				val doc = Jsoup.parse(loginPageHtml, loginUrl)

				// 3. 提取表单信息
				val form = doc.selectFirst("form")
				if (form == null) {
					Log.e("Login", "No form found in login page")
					return@withContext false
				}

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

				// 4. 查找并下载验证码
				val captchaBitmap = downloadImage("$baseUrl/captcha.jpg?vpn-1")
				if (captchaBitmap == null) {
					Log.e("Login", "Failed to download captcha")
					return@withContext false
				}

				// 5. 识别验证码
				val captchaResult = CaptchaSolver.solve(captchaBitmap)
				if (captchaResult == null) {
					Log.e("Login", "OCR returned null")
					return@withContext false
				}
				Log.i("Login", "Solved Captcha: $captchaResult")

				// 6. preparing data
				val encryptedPwd = RsaCrypto.encryptPassword(password, publicKeyPem)

				// 更新 map 中的关键字段
				inputFields["username"] = username
				inputFields["password"] = encryptedPwd
				inputFields["authcode"] = captchaResult

				val formBuilder = FormBody.Builder()
				// 7. 填入所有字段
				inputFields.forEach { (name, value) ->
					formBuilder.add(name, value)
				}

				// 8. 提交登录
				val postReq = Request.Builder()
					.url(submitUrl)
					.method("POST", formBuilder.build())
					.build()

				val success = client.newCall(postReq).execute().use { resp ->
					Log.i("Login", "Final URL: ${resp.request.url}")
					resp.isSuccessful && checkLoginSuccess()
				}

				return@withContext success

			} catch (e: Exception) {
				Log.e("Login", "Auto login error", e)
				return@withContext false
			}
		}
	}

	private fun getUrlContent(url: String): String? {
		val req = Request.Builder().url(url).get().build()
		return try {
			client.newCall(req).execute().use { resp ->
				if (resp.isSuccessful) {
					resp.body.string()
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
				// 判断逻辑：请求 HomeUrl，看是否跳转回登录页，或内容包含登录成功标识
				checkLoginSuccess()
			} catch (e: Exception) {
				e.printStackTrace()
				false
			}
		}
	}

	private fun checkLoginSuccess(): Boolean {
		// 请求主页，检查是否包含"你好"关键词
		val req = Request.Builder().url("$homeUrl/Home").get().build()

		return client.newCall(req).execute().use { resp ->
			val body = resp.body.string()
			!body.contains("login_box")
		}
	}

	private fun downloadImage(url: String): Bitmap? {
		val req = Request.Builder().url(url).get().build()
		return try {
			client.newCall(req).execute().use { resp ->
				if (resp.isSuccessful) {
					resp.body.byteStream().use { stream ->
						BitmapFactory.decodeStream(stream)
					}
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
				"${homeUrl}/Home/GetCurriculumTable?vpn-12-o2-jwstu.shsmu.edu.cn=&Start=$start&End=$end"

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