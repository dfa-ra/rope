package app.rope.android.update

/**
 * Identity blobs and GitHub tokens must not land in shared Downloads.
 * APK copies may still go there so a signing-key conflict has an installer file.
 */
object PublicBackupRules {
    const val allowIdentityDump = false
    const val allowAutoRestore = false
    const val allowApkCopy = true
}
