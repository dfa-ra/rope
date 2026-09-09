package app.rope.android.data

/**
 * Group polls: type=2 `kind=poll` (no object blob) + type=5 `vote` / `poll_close`.
 * Tallies stay in ciphertext. Named votes — envelope sender_id is visible.
 */
data class PollReceipt(
    val id: String,
    val qzid: String,
    val voterId: String,
    val voterName: String,
    val kind: String,
    val op: String,
    val indexes: List<Int>,
    val timestampMs: Long,
)

data class PollVoter(
    val id: String,
    val name: String,
)

data class PollState(
    val qzid: String,
    val question: String,
    val options: List<String>,
    val multi: Boolean,
    val closed: Boolean,
    val closedBy: String? = null,
    val counts: List<Int> = emptyList(),
    val myIndexes: Set<Int> = emptySet(),
    val votersByOption: List<List<PollVoter>> = emptyList(),
    val totalVoters: Int = 0,
)

object PollRules {
    const val KIND = "poll"
    const val Q_MAX = 240
    const val OPT_MIN = 2
    const val OPT_MAX = 10
    const val OPT_LEN = 100
    const val ANONYMOUS = false

    fun validate(question: String, options: List<String>): Boolean {
        val q = question.trim()
        if (q.isEmpty() || q.length > Q_MAX) return false
        val opts = cleanOptions(options)
        if (opts.size !in OPT_MIN..OPT_MAX) return false
        return opts.all { it.length in 1..OPT_LEN }
    }

    fun cleanOptions(options: List<String>): List<String> =
        options.map { it.trim() }.filter { it.isNotEmpty() }.take(OPT_MAX)

    fun preview(question: String): String {
        val q = question.trim()
        return if (q.isEmpty()) "Опрос" else "Опрос: $q"
    }

    fun footer(totalVoters: Int, closed: Boolean): String {
        val base = "$totalVoters голосов · видны"
        return if (closed) "$base · опрос закрыт" else base
    }

    fun canCreate(inGroup: Boolean, isMember: Boolean): Boolean = inGroup && isMember

    fun canVote(isMember: Boolean, closed: Boolean): Boolean = isMember && !closed

    fun canClose(
        voterId: String,
        authorId: String,
        organizerId: String,
        isOwner: Boolean,
        isMember: Boolean,
    ): Boolean {
        if (!isMember || voterId.isBlank()) return false
        if (voterId == authorId) return true
        if (voterId == organizerId) return true
        return isOwner
    }

    fun closerAllowed(
        voterId: String,
        authorId: String,
        organizerId: String,
        ownerIds: Set<String>,
    ): Boolean {
        if (voterId.isBlank()) return false
        if (voterId == authorId || voterId == organizerId) return true
        return voterId in ownerIds
    }

    /** Empty list means withdraw (`op=clear`). */
    fun nextIx(multi: Boolean, current: List<Int>, tapped: Int): List<Int> {
        if (tapped < 0) return current.distinct().sorted()
        return if (multi) {
            val set = current.toMutableSet()
            if (!set.add(tapped)) set.remove(tapped)
            set.sorted()
        } else {
            if (current.size == 1 && current.first() == tapped) emptyList() else listOf(tapped)
        }
    }

    fun qzidOf(msg: ChatMessage): String {
        if (msg.extra.isNotBlank()) {
            val qzid = runCatching { MediaPayload.parse(msg.extra).pollQzid }.getOrNull()
            if (!qzid.isNullOrBlank()) return qzid
        }
        return msg.id
    }

    fun apply(
        question: String,
        options: List<String>,
        multi: Boolean,
        qzid: String,
        receipts: List<PollReceipt>,
        selfId: String,
        authorId: String,
        organizerId: String,
        ownerIds: Set<String> = emptySet(),
    ): PollState {
        val opts = cleanOptions(options)
        val n = opts.size
        val ordered = receipts
            .filter { it.qzid == qzid }
            .sortedWith(compareBy<PollReceipt> { it.timestampMs }.thenBy { it.id })
        var closed = false
        var closedBy: String? = null
        val lastVote = linkedMapOf<String, PollReceipt>()
        for (r in ordered) {
            when (r.kind) {
                ChatControl.POLL_CLOSE -> {
                    if (!closed && closerAllowed(r.voterId, authorId, organizerId, ownerIds)) {
                        closed = true
                        closedBy = r.voterId
                    }
                }
                ChatControl.VOTE -> {
                    if (closed) continue
                    lastVote[r.voterId] = r
                }
            }
        }
        val counts = IntArray(n)
        val voters = List(n) { mutableListOf<PollVoter>() }
        var total = 0
        var mine = emptySet<Int>()
        for (vote in lastVote.values) {
            if (vote.op == ChatControl.OP_CLEAR || vote.indexes.isEmpty()) continue
            val ix = vote.indexes.filter { it in 0 until n }.distinct()
            if (ix.isEmpty()) continue
            total += 1
            val name = vote.voterName.ifBlank { vote.voterId.take(8) }
            val voter = PollVoter(vote.voterId, name)
            for (i in ix) {
                counts[i] += 1
                voters[i] += voter
            }
            if (vote.voterId == selfId) mine = ix.toSet()
        }
        return PollState(
            qzid = qzid,
            question = question.trim(),
            options = opts,
            multi = multi,
            closed = closed,
            closedBy = closedBy,
            counts = counts.toList(),
            myIndexes = mine,
            votersByOption = voters,
            totalVoters = total,
        )
    }
}
