package org.parser.combinators

typealias Continuation<N> = (N) -> Unit

private fun <T> memoK(k: Continuation<T>): Continuation<T> {
    val s = HashSet<T>()
    return { t ->
        if (t !in s) {
            s += t; k(t)
        }
    }
}

/** The [ParserResult] class represent all results of parsing. It uses Continuation-passing style. */
@JvmInline
value class ParserResult<T>(val invoke: (Trampoline, Continuation<T>) -> Unit) {

    fun getResults(): List<T> {
        val results = ArrayList<T>()
        val trampoline = Trampoline()
        this.invoke(trampoline) {
            results.add(it)
        }
        trampoline.runLoop()

        return results
    }

    fun <T2> map(transform: (T) -> T2): ParserResult<T2> {
        return ParserResult { trampoline, k ->
            val k2: Continuation<T> = { t ->
                val t2 = transform(t)
                trampoline.call {
                    k(t2)
                }
            }
            this.invoke(trampoline, memoK(k2))
        }
    }

    fun <T2> flatMap(transform: (T) -> ParserResult<T2>): ParserResult<T2> {
        return ParserResult { trampoline, k ->
            val resK: Continuation<T> = { t1 ->
                transform(t1).invoke(trampoline) { t2 ->
                    trampoline.call {
                        k(t2)
                    }
                }
            }
            this.invoke(trampoline, memoK(resK))
        }
    }

    /** Combines results with [ParserResult] that [nextRes] returns. */
    fun orElse(nextRes: () -> ParserResult<T>): ParserResult<T> {
        return ParserResult { trampoline, k ->
            this.invoke(trampoline, k)
            nextRes().invoke(trampoline, k)
        }
    }

    companion object {
        /** Returns [ParserResult] that contains only [t] result. */
        fun <T> success(t: T): ParserResult<T> = ParserResult { trampoline, k -> k(t) }

        /** Returns [ParserResult] that contains no results. */
        fun <T> failure(): ParserResult<T> = ParserResult { _, _ -> }

        /** Returns [ParserResult] that memoizes all continuations and calls them on any unique result returned by [res()][res].  */
        internal fun <T> memoResult(res: () -> ParserResult<T>): ParserResult<T> {
            var results: LinkedHashSet<T>? = null
            var continuations: ArrayList<Continuation<T>>? = null
            return ParserResult { trampoline, k ->
                val capturedContinuations = continuations ?: ArrayList<Continuation<T>>(1)
                continuations = capturedContinuations
                if (capturedContinuations.isEmpty()) {
                    capturedContinuations.add(k)
                    val p = res()
                    p.invoke(trampoline) { t ->
                        val capturedResults = results ?: LinkedHashSet<T>(1)
                        results = capturedResults
                        if (!capturedResults.contains(t)) {
                            capturedResults.add(t)
                            for (continuation in capturedContinuations) {
                                trampoline.call { continuation(t) }
                            }
                        }
                    }
                } else {
                    capturedContinuations.add(k)
                    for (r in results ?: listOf()) {
                        trampoline.call { k(r) }
                    }
                }
            }
        }
    }
}






