package com.neo.launcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Http {
    private const val MAX_BYTES = 8 * 1024 * 1024

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 10000
            useCaches = false
            instanceFollowRedirects = true
        }

    fun getBytes(url: String): ByteArray {
        val c = open(url)
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            val bytes = c.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > MAX_BYTES) throw IOException("Fișier prea mare")
                }
                out.toByteArray()
            }
            return bytes
        } finally {
            c.disconnect()
        }
    }

    fun get(url: String): String = String(getBytes(url), Charsets.UTF_8)

    fun put(url: String, body: String) {
        val c = open(url)
        try {
            c.requestMethod = "PUT"
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            c.responseCode
        } finally {
            c.disconnect()
        }
    }

    fun bitmap(url: String, maxPx: Int): Bitmap? = try {
        val bytes = getBytes(url)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxPx * 2 || bounds.outHeight / sample > maxPx * 2) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (e: Exception) {
        null
    }
}
