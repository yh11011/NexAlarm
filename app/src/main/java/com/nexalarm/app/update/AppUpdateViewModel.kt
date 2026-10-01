package com.nexalarm.app.update

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexalarm.app.BuildConfig
import com.nexalarm.app.data.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {
    data class State(val phase: String = "idle", val release: ReleaseInfo? = null, val progress: Float = 0f)
    private val mutable = MutableStateFlow(State())
    val state = mutable.asStateFlow()
    private val app: Application get() = getApplication()
    private val apk get() = File(app.cacheDir, "updates/latest.apk")

    @Suppress("DEPRECATION")
    private fun signerDigests(info: PackageInfo): Set<String> {
        val certificates = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return certificates.orEmpty().map { digest(it.toByteArray()) }.toSet()
    }

    @Suppress("DEPRECATION")
    private fun installedSigners(): Set<String> = signerDigests(app.packageManager.getPackageInfo(app.packageName,
        if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES))

    fun check() {
        if (mutable.value.phase in setOf("checking", "downloading")) return
        mutable.value = State("checking")
        viewModelScope.launch {
            val result = runCatching {
                val response = ApiClient.get("https://login.nex11.me/api/v1/app/releases/latest?channel=beta")
                check(response.code in 200..299)
                val json = JSONObject(response.body)
                if (!json.getBoolean("available")) return@runCatching State("unavailable")
                val release = ReleaseInfo(json.getString("package_name"), json.getString("version_name"), json.getLong("version_code"),
                    json.getInt("min_sdk"), json.getLong("size_bytes"), json.getString("sha256"), json.getString("signer_sha256"),
                    json.getString("apk_url"), json.getString("release_url"), json.optString("notes"))
                if (release.versionCode <= BuildConfig.VERSION_CODE) State("current", release)
                else if (!UpdatePolicy.compatible(release, app.packageName, BuildConfig.VERSION_CODE.toLong(), Build.VERSION.SDK_INT, installedSigners())) State("incompatible", release)
                else State("available", release)
            }
            mutable.value = result.getOrElse { State("check_failed") }
        }
    }

    fun download(context: Context) {
        val release = mutable.value.release ?: return
        if (mutable.value.phase !in setOf("available", "download_failed", "install_failed")) return
        mutable.value = State("downloading", release)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    check(UpdatePolicy.compatible(release, app.packageName, BuildConfig.VERSION_CODE.toLong(), Build.VERSION.SDK_INT, installedSigners()))
                    apk.parentFile!!.mkdirs()
                    val part = File(apk.parentFile, "latest.part")
                    try {
                        val connection = downloadConnection(release.apkUrl)
                        try {
                            check(connection.responseCode == 200)
                            var copied = 0L
                            val hash = MessageDigest.getInstance("SHA-256")
                            connection.inputStream.use { input -> part.outputStream().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    copied += count
                                    check(copied <= release.size)
                                    hash.update(buffer, 0, count)
                                    output.write(buffer, 0, count)
                                    mutable.value = State("downloading", release, copied.toFloat() / release.size)
                                }
                            } }
                            check(copied == release.size && hex(hash.digest()) == release.sha256.lowercase())
                            verifyArchive(part, release)
                            check(part.renameTo(apk))
                        } finally { connection.disconnect() }
                    } finally { part.delete() }
                }
            }
            mutable.value = State(if (result.isSuccess) "ready" else "download_failed", release)
            if (result.isSuccess) install(context)
        }
    }

    private fun downloadConnection(initial: String): HttpURLConnection {
        var url = initial
        repeat(6) {
            check(UpdatePolicy.safeDownloadUrl(url))
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            val code = connection.responseCode
            if (code !in setOf(301, 302, 303, 307, 308)) return connection
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            check(location != null)
            url = URL(URL(url), location).toString()
        }
        error("Too many redirects")
    }

    @Suppress("DEPRECATION")
    private fun verifyArchive(file: File, release: ReleaseInfo) {
        val info = app.packageManager.getPackageArchiveInfo(file.path,
            if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
            ?: error("Invalid APK")
        val version = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        check(info.packageName == app.packageName && version == release.versionCode && info.versionName == release.versionName)
        check(info.applicationInfo?.minSdkVersion == release.minSdk && release.minSdk <= Build.VERSION.SDK_INT)
        check(signerDigests(info) == setOf(release.signerSha256.lowercase()) && signerDigests(info) == installedSigners())
    }

    fun install(context: Context) {
        val release = mutable.value.release ?: return
        if (mutable.value.phase !in setOf("ready", "permission_required", "install_failed")) return
        if (!app.packageManager.canRequestPackageInstalls()) {
            runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))) }
                .onSuccess { mutable.value = State("permission_required", release) }
                .onFailure { mutable.value = State("install_failed", release) }
            return
        }
        viewModelScope.launch {
            val valid = runCatching { withContext(Dispatchers.IO) {
                check(apk.length() == release.size && digest(apk.readBytes()) == release.sha256.lowercase())
                verifyArchive(apk, release)
            } }
            if (valid.isFailure) { mutable.value = State("download_failed", release); return@launch }
            val result = runCatching {
                val uri = FileProvider.getUriForFile(context, "${app.packageName}.updates", apk)
                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
            mutable.value = State(if (result.isSuccess) "ready" else "install_failed", release)
        }
    }

    private fun digest(bytes: ByteArray) = hex(MessageDigest.getInstance("SHA-256").digest(bytes))
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
}
