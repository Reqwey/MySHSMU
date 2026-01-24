package xyz.reqwey.myshsmu.utils

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

object RsaCrypto {
	fun loadPublicKeyFromPem(pem: String): PublicKey {
		val clean = pem
			.replace("-----BEGIN PUBLIC KEY-----", "")
			.replace("-----END PUBLIC KEY-----", "")
			.replace("\\s".toRegex(), "")
		val decoded = Base64.decode(clean, Base64.DEFAULT)
		val spec = X509EncodedKeySpec(decoded)
		val kf = KeyFactory.getInstance("RSA")
		return kf.generatePublic(spec)
	}

	fun encryptPassword(password: String, publicKeyPem: String): String {
		val pub = loadPublicKeyFromPem(publicKeyPem)
		val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
		cipher.init(Cipher.ENCRYPT_MODE, pub)
		val out = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
		return Base64.encodeToString(out, Base64.NO_WRAP)
	}
}