package app.rope.android

/** HTTP join is emulator/debug only. Release NSC forbids cleartext. */
object JoinDebugRules {
    fun showHttpJoin(debugBuild: Boolean): Boolean = debugBuild
}
