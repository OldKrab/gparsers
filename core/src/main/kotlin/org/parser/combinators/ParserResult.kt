package org.parser.combinators

typealias Continuation<N> = (N) -> Unit

/** The [ParserResult] class represent all results of parsing. It uses Continuation-passing style. */
@JvmInline
value class ParserResult<T>(val invoke: (Continuation<T>) -> Unit) {

    fun getResults(): List<T> {
        val results = ArrayList<T>()
        Trampoline.call {
            this.invoke {
                results.add(it)
            }
        }
        try{
            Trampoline.runLoop()

        }finally {
            Trampoline.calls.clear()

        }
        return results
    }

    fun <T2> map(transform: (T) -> T2): ParserResult<T2> {
        return ParserResult { k ->
            val k2: Continuation<T> = { t ->
                val t2 = transform(t)
                Trampoline.call {
                    k(t2)
                }
            }
            this.invoke(k2)
        }
    }

    fun <T2> flatMap(transform: (T) -> ParserResult<T2>): ParserResult<T2> {
        return ParserResult { k ->
            val resK: Continuation<T> = { t1 ->
                transform(t1).invoke { t2 ->
                    k(t2)
                }
            }
            this.invoke(resK)
        }
    }

    /** Combines results with [ParserResult] that [nextRes] returns. */
    fun orElse(nextRes: () -> ParserResult<T>): ParserResult<T> {
        return ParserResult { k ->
            this.invoke(k)
            nextRes().invoke(k)
        }
    }

    companion object {
        /** Returns [ParserResult] that contains only [t] result. */
        fun <T> success(t: T): ParserResult<T> = ParserResult { k -> k(t) }

        /** Returns [ParserResult] that contains no results. */
        fun <T> failure(): ParserResult<T> = ParserResult { _ -> }

        /** Returns [ParserResult] that memoizes all continuations and calls them on any unique result returned by [res()][res].  */
        internal fun <T> memoResult(res: () -> ParserResult<T>): ParserResult<T> {
            val results = LinkedHashSet<T>()
            val continuations = ArrayList<Continuation<T>>()
            return ParserResult { k ->
                if (continuations.isEmpty()) {
                    continuations.add(k)
                    val p = res()
                    p.invoke { t ->
                        if (!results.contains(t)) {
                            results.add(t)
                            for (continuation in continuations.reversed()) {
                                continuation(t)
                            }
                        }
                    }
                } else {
                    continuations.add(k)
                    for (r in results) {
                        k(r)
                    }
                }
            }
        }
    }
}






