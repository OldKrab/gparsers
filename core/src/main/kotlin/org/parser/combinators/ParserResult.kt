package org.parser.combinators

typealias Continuation<N> = (N) -> Unit

@JvmInline
value class ParserResult<T>(val invoke: (Continuation<T>) -> Unit) {
    fun getResults(): List<T>{
        val results = ArrayList<T>()
        this.invoke {
            results.add(it)
        }
        return results
    }

    fun <T2> map(transform: (T) -> T2): ParserResult<T2> {
        return ParserResult { k ->
            val k2: Continuation<T> = { t ->
                val t2 = transform(t)
                k(t2)
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

    fun orElse(nextRes: () -> ParserResult<T>): ParserResult<T> {
        return ParserResult { k ->
            this.invoke(k)
            nextRes().invoke(k)
        }
    }

    companion object {
        fun <T> success(t: T): ParserResult<T> = ParserResult { k -> k(t) }

        fun <T> failure(): ParserResult<T> = ParserResult { _ -> }
    }
}






