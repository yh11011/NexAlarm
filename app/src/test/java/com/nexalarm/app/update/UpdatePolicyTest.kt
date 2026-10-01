package com.nexalarm.app.update

import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    private val release = ReleaseInfo("com.nexalarm.app", "1.2.0-beta.1", 42, 26, 100,
        "a".repeat(64), "b".repeat(64),
        "https://github.com/yh11011/NexAlarm/releases/download/v1.2.0-beta.1/NexAlarm-v1.2.0-beta.1.apk",
        "https://github.com/yh11011/NexAlarm/releases/tag/v1.2.0-beta.1", "Changes")
    private fun allowed(candidate: ReleaseInfo = release, installed: Long = 2) =
        UpdatePolicy.compatible(candidate, "com.nexalarm.app", installed, 35, setOf("b".repeat(64)))

    @Test fun newerReleaseWithMatchingCertificateIsAllowed() { assertTrue(allowed()) }
    @Test fun downgradesAndSameVersionAreRejected() { assertFalse(allowed(installed = 42)); assertFalse(allowed(installed = 50)) }
    @Test fun packageSignatureAndDigestMustMatch() {
        assertFalse(allowed(release.copy(packageName = "another.app")))
        assertFalse(allowed(release.copy(signerSha256 = "c".repeat(64))))
        assertFalse(allowed(release.copy(sha256 = "bad")))
    }
    @Test fun unsupportedAndroidAndUntrustedSourcesAreRejected() {
        assertFalse(allowed(release.copy(minSdk = 36)))
        assertFalse(allowed(release.copy(apkUrl = "https://example.com/app.apk")))
        assertFalse(allowed(release.copy(size = 0)))
        assertFalse(UpdatePolicy.safeDownloadUrl("http://github.com/app.apk"))
        assertFalse(UpdatePolicy.safeDownloadUrl("https://github.com.evil.example/app.apk"))
        assertFalse(UpdatePolicy.safeDownloadUrl("https://user@github.com/app.apk"))
        assertTrue(UpdatePolicy.safeDownloadUrl("https://release-assets.githubusercontent.com/asset?signature=abc"))
    }
}
