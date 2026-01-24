package xyz.reqwey.myshsmu.utils

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

object CaptchaSolver {
	private const val TAG = "CaptchaSolver"

	// 初始化识别器 (通用拉丁字母模型即可识别数字和算式)
	private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

	/**
	 * 识别图片并返回计算结果
	 */
	suspend fun solve(bitmap: Bitmap): String? {
		return try {
			val image = InputImage.fromBitmap(bitmap, 0)
			// 使用 await() 将异步 Task 转换为协程
			val visionText = recognizer.process(image).await()
			val rawText = visionText.text

			Log.d(TAG, "OCR Original Result: $rawText")

			// 清洗文本，只保留数字和运算符
			val cleanText = cleanText(rawText)
			Log.d(TAG, "OCR Cleaned Text: $cleanText")

			// 计算结果
			calculateExpression(cleanText)
		} catch (e: Exception) {
			Log.e(TAG, "OCR Failed", e)
			null
		}
	}

	private fun cleanText(text: String): String {
		// 1. 预处理：移除空格
		var t = text.replace("\\s+".toRegex(), "")

		// 2. 字符映射：纠正 OCR 常见的混淆 (将类似形状的字母转为白名单内的字符)
		t = t.replace('o', '0')
			.replace('O', '0')
			.replace('l', '1')
			.replace('i', '1')
			.replace('I', '1')
			.replace('z', '2')
			.replace('Z', '2')
			.replace('s', '5')
			.replace('S', '5')
			.replace('b', '6')
			.replace('h', '6')
			.replace('B', '8')
			.replace('g', '9') // g sometimes looks like 9
			// 符号修正
			.replace('x', '*')
			.replace('X', '*')
			.replace('÷', '/')
			.replace(':', '=') // : like =
			.replace('_', '-') // _ like -

		// 3. 严格过滤：只保留 用户要求的字符集 [数字, +, -, *, /, =, ?]
		val p = Pattern.compile("[^0-9+\\-*/=?]")
		return p.matcher(t).replaceAll("")
	}

	/**
	 * 简单的表达式计算 (e.g., "1+2=?" -> "3")
	 */
	private fun calculateExpression(expression: String): String? {
		if (expression.isBlank()) return null

		try {
			// 移除末尾的 ? 或 = 或 -（"="识别错误）
			val expr = expression.trimEnd('=', '?', '-')

			// 寻找运算符
			val operators = charArrayOf('+', '-', '*', '/')
			var operator = ' '
			var opIndex = -1

			for (op in operators) {
				opIndex = expr.indexOf(op)
				if (opIndex != -1) {
					operator = op
					break
				}
			}

			if (opIndex == -1) {
				// 没有运算符，直接返回数字 (有时验证码就是单纯的数字)
				return if (expr.all { it.isDigit() }) expr else null
			}

			val leftStr = expr.take(opIndex)
			val rightStr = expr.substring(opIndex + 1)

			val left = leftStr.toInt()
			val right = rightStr.toInt()

			return when (operator) {
				'+' -> (left + right).toString()
				'-' -> (left - right).toString()
				'*' -> (left * right).toString()
				'/' -> if (right != 0) (left / right).toString() else null
				else -> null
			}
		} catch (e: Exception) {
			Log.e(TAG, "Calculation Error: $expression", e)
			return null
		}
	}
}