package com.neo.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import java.util.concurrent.Executors

data class AppItem(
    val label: String,
    val pkg: String,
    val cls: String,
    val user: UserHandle,
    val icon: Drawable
) {
    val key: String get() = "$pkg/$cls"
    val component: ComponentName get() = ComponentName(pkg, cls)
}

object AppRepo {
    @Volatile
    var apps: List<AppItem> = emptyList()
        private set

    private val exec = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun load(ctx: Context, cb: (List<AppItem>) -> Unit) {
        val app = ctx.applicationContext
        exec.execute {
            val list: List<AppItem> = try {
                val la = app.getSystemService(LauncherApps::class.java)
                la.getActivityList(null, Process.myUserHandle())
                    .filter { it.componentName.packageName != app.packageName }
                    .map {
                        AppItem(
                            it.label?.toString() ?: it.componentName.packageName,
                            it.componentName.packageName,
                            it.componentName.className,
                            it.user,
                            it.getBadgedIcon(0)
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            } catch (e: Exception) {
                emptyList()
            }
            main.post {
                apps = list
                cb(list)
            }
        }
    }

    /** Telefon, mesaje, browser, cameră — pentru dock-ul inițial. */
    fun defaultDock(ctx: Context, list: List<AppItem>): List<String> {
        val pm = ctx.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        )
        val out = ArrayList<String>()
        for (i in intents) {
            val pkg = try {
                pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            } catch (e: Exception) {
                null
            } ?: continue
            list.firstOrNull { it.pkg == pkg && it.key !in out }?.let { out.add(it.key) }
        }
        for (a in list) {
            if (out.size >= 4) break
            if (a.key !in out) out.add(a.key)
        }
        return out.take(4)
    }
}
