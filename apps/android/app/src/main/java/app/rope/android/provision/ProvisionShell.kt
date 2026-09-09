package app.rope.android.provision

/** Quote/validate values interpolated into the remote install shell. */
object ProvisionShell {
    fun quote(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    fun hostOK(host: String): Boolean {
        val h = host.trim()
        if (h.isEmpty() || h.length > 253) return false
        return h.all { ch ->
            ch.isLetterOrDigit() || ch == '.' || ch == '-' || ch == ':' || ch == '[' || ch == ']'
        }
    }

    fun portOK(port: Int): Boolean = port in 1..65535
}
