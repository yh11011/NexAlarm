package com.nexalarm.app.update

import java.net.URI

data class ReleaseInfo(
    val packageName: String, val versionName: String, val versionCode: Long,
    val minSdk: Int, val size: Long, val sha256: String, val signerSha256: String,
    val apkUrl: String, val releaseUrl: String, val notes: String
)

object UpdatePolicy {
    const val SOURCE = "https://github.com/yh11011/NexAlarm/releases"
    fun compatible(release: ReleaseInfo, packageName: String, installedVersion: Long, sdk: Int, signers: Set<String>): Boolean =
        release.packageName == packageName && release.versionCode > installedVersion && release.minSdk <= sdk &&
            release.size in 1..(200L * 1024 * 1024) &&
            release.sha256.matches(Regex("[a-fA-F0-9]{64}")) && release.signerSha256.lowercase() in signers &&
            release.apkUrl.matches(Regex("https://github\\.com/yh11011/NexAlarm/releases/download/v[0-9]+\\.[0-9]+\\.[0-9]+-beta\\.[0-9]+/NexAlarm-v[0-9]+\\.[0-9]+\\.[0-9]+-beta\\.[0-9]+\\.apk")) &&
            release.releaseUrl.matches(Regex("https://github\\.com/yh11011/NexAlarm/releases/tag/v[0-9]+\\.[0-9]+\\.[0-9]+-beta\\.[0-9]+"))

    fun safeDownloadUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443) &&
            uri.host in setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com", "github-releases.githubusercontent.com")
    }.getOrDefault(false)
}
