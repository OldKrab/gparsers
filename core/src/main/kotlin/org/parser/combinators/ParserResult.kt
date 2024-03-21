package org.parser.combinators

typealias Continuation<OutS, R> = (OutS, R) -> Unit

@JvmInline
value class ParserResult<S, R>(private val f: (Continuation<S, R>) -> Unit) {
    fun apply(k: Continuation<S, R>) {
        f(k)
    }

    fun <S2, R2> map(transform: (S, R) -> Pair<S2, R2>): ParserResult<S2, R2> {
        return ParserResult { k ->
            val k2: Continuation<S, R> = { s, r ->
                val (s2, r2) = transform(s, r)
                k(s2, r2)
            }
            this.apply(k2)
        }
    }

    fun <S2, R2> flatMap(transform: (S) -> ParserResult<S2, R2>): ParserResult<S2, Pair<R, R2>> {
        return ParserResult { k ->
            val resK: Continuation<S, R> = { s, r1 ->
                transform(s).apply { s2, r2 ->
                    k(s2, Pair(r1, r2))
                }
            }
            this.apply(resK)
        }
    }

    fun  orElse(nextRes: () -> ParserResult<S, R>): ParserResult<S, R> {
        return ParserResult { k ->
            this.apply(k)
            nextRes().apply(k)
        }
    }

    companion object {
        fun <S, R> success(s: S, v: R): ParserResult<S, R> = ParserResult { k -> k(s, v) }

        fun <S, R> failure(): ParserResult<S, R> = ParserResult { _ -> }
    }
}






