package xyz.reqwey.myshsmu.network

import android.content.Context
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkModule {

	// 改为 lateinit，需要在 App 启动时初始化
	lateinit var cookieJar: PersistentCookieJar
	lateinit var client: OkHttpClient

	fun init(context: Context) {
		cookieJar = PersistentCookieJar(context)

		client = OkHttpClient.Builder()
			.cookieJar(cookieJar)
			.connectTimeout(15, TimeUnit.SECONDS)
			.readTimeout(15, TimeUnit.SECONDS)
			.writeTimeout(15, TimeUnit.SECONDS)
			// 可选：添加 Log 拦截器方便调试
			.build()
	}
}