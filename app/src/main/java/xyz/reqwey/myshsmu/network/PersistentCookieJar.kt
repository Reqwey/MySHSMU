package xyz.reqwey.myshsmu.network

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject

class PersistentCookieJar(context: Context) : CookieJar {

	private val prefs: SharedPreferences =
		context.getSharedPreferences("CookiePrefs", Context.MODE_PRIVATE)
	private val memoryCache = mutableMapOf<String, MutableList<Cookie>>()

	init {
		// App 启动时，将所有本地 Cookie 加载到内存缓存中
		loadAllCookiesFromDisk()
	}

	@Synchronized
	override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
		val hostKey = url.host
		val currentList = memoryCache.getOrPut(hostKey) { mutableListOf() }

		// 更新内存
		cookies.forEach { cookie ->
			// 移除旧的同名 Cookie
			currentList.removeAll { it.name == cookie.name && it.domain == cookie.domain }
			currentList.add(cookie)
		}

		// 保存到本地磁盘
		persistCookies(hostKey, currentList)
	}

	@Synchronized
	override fun loadForRequest(url: HttpUrl): List<Cookie> {
		val hostKey = url.host
		val cookies = memoryCache[hostKey].orEmpty()

		// 过滤过期 Cookie
		val validCookies = cookies.filter { it.expiresAt > System.currentTimeMillis() }

		// 如果有过期被过滤掉的，可以考虑在这里触发一次清理磁盘（可选优化）
		return validCookies
	}

	private fun persistCookies(host: String, cookies: List<Cookie>) {
		val jsonArray = JSONArray()
		cookies.forEach { cookie ->
			val json = JSONObject().apply {
				put("name", cookie.name)
				put("value", cookie.value)
				put("domain", cookie.domain)
				put("path", cookie.path)
				put("expiresAt", cookie.expiresAt)
				put("secure", cookie.secure)
				put("httpOnly", cookie.httpOnly)
				put("hostOnly", cookie.hostOnly)
			}
			jsonArray.put(json)
		}
		prefs.edit { putString(host, jsonArray.toString()) }
	}

	private fun loadAllCookiesFromDisk() {
		val allEntries = prefs.all
		for ((host, value) in allEntries) {
			if (value !is String) continue
			try {
				val jsonArray = JSONArray(value)
				val list = mutableListOf<Cookie>()
				for (i in 0 until jsonArray.length()) {
					val obj = jsonArray.getJSONObject(i)
					val builder = Cookie.Builder()
						.name(obj.getString("name"))
						.value(obj.getString("value"))
						.domain(obj.getString("domain"))
						.path(obj.getString("path"))
						.expiresAt(obj.getLong("expiresAt"))

					if (obj.optBoolean("secure")) builder.secure()
					if (obj.optBoolean("httpOnly")) builder.httpOnly()
					if (obj.optBoolean("hostOnly")) builder.hostOnlyDomain(obj.getString("domain"))

					list.add(builder.build())
				}
				memoryCache[host] = list
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	// 清空 Cookies (如退出登录时)
	fun clear() {
		memoryCache.clear()
		prefs.edit { clear() }
	}
}