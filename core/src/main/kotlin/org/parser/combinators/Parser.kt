package org.parser.combinators



class Parser<E, in InS, OutS, R> private constructor(
    private val inner: (E, InS) -> ParserResult<OutS, R>,
    private val name: String
) {
    class StopByLimitException : Exception("Stop parsing by limit")
    fun parse(env: E, input: InS): ParserResult<OutS, R> = inner(env, input)
    fun getResults(env: E, input: InS, count: Int = -1): List<Pair<OutS, R>> {
        val res = ArrayList<Pair<OutS, R>>()
        try {
            this.parse(env, input).apply { s, r ->
                res.add(Pair(s, r))
                if (res.size == count)
                    throw StopByLimitException()
            }
        } catch (_: StopByLimitException) {
        }

        return res
    }

    override fun toString(): String {
        return name
    }

    companion object {
        fun <E, InS, OutS, R> make(name: String, inner: (E, InS) -> ParserResult<OutS, R>): Parser<E, InS, OutS, R> {
            return memo(Parser(inner, name))
        }

        private fun <E, InS, OutS, R> memo(parser: Parser<E, InS, OutS, R>): Parser<E, InS, OutS, R> {
            val table = HashMap<Pair<E, InS>, ParserResult<OutS, R>>()
            return Parser(
                { env, inS -> table.getOrPut(Pair(env, inS)) { memoResult { parser.parse(env, inS) } } },
                parser.name
            )
        }

        private fun <S, R> memoResult(res: () -> ParserResult<S, R>): ParserResult<S, R> {
            val results = ArrayList<Pair<S, R>>()
            val continuations = ArrayList<Continuation<S, R>>()
            return ParserResult { k ->
                val isFirstCall = continuations.isEmpty()
                continuations.add(k)
                if (isFirstCall) {
                    res().apply { s, r ->
                        val newState = Pair(s, r)
                        if (!results.contains(newState)) {
                            results.add(newState)
                            for (continuation in continuations) {
                                continuation(s, r)
                            }
                        }
                    }
                } else {
                    for ((s, r) in results) {
                        k(s, r)
                    }
                }
            }
        }
    }
}



