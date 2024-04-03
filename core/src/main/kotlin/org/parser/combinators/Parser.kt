package org.parser.combinators

import org.parser.sppf.SPPF
import org.parser.sppf.node.NonPackedNode

interface BaseParser
class Parser<E, InS, OutS, R> private constructor(
    var inner: (E, SPPF<E>, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>,
    var name: String
) : BaseParser {
    fun parse(env: E, sppf: SPPF<E>, input: InS): ParserResult<NonPackedNode<InS, OutS, R>> =
        inner(env, sppf, input)

    override fun toString(): String {
        return name
    }

    companion object {
        fun <E, InS, OutS, R> make(name: String, inner: (E, SPPF<E>, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>): Parser<E, InS, OutS, R> {
        return Parser(
            { env, sppf, inS ->  inner(env, sppf, inS)  },
            name
        )
        }
        fun <E, InS, OutS, R> memo(name: String, inner: (E, SPPF<E>, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>): Parser<E, InS, OutS, R> {
            val table = HashMap<Pair<E, InS>, ParserResult<NonPackedNode<InS, OutS, R>>>()
            val res: Parser<E, InS, OutS, R> = Parser(
                { env, sppf, inS -> table.computeIfAbsent(Pair(env, inS)) { memoResult(name) { inner(env, sppf, inS) } } },
                name
            )
            return res
        }

        private fun <T> memoResult(name: String, res: () -> ParserResult<T>): ParserResult<T> {
            val results = ArrayList<T>()
            val continuations = ArrayList<Continuation<T>>()
            return ParserResult { k ->
                val name = name
                if (continuations.isEmpty()) {
                    continuations.add(k)
                    val p = res()
                    p.invoke { t ->
                        val p = p
                        if (!results.contains(t)) {
                            results.add(t)
                            for (continuation in continuations) {
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

fun <E, I, O, R> applyParser(env: E, parser: Parser<E, I, O, R>, inState: I, count: Int = -1): List<NonPackedNode<I, O, R>> {
    val sppf = SPPF<E>()
    val res = parser.parse(env, sppf, inState)
    val trees = ArrayList<NonPackedNode<I, O, R>>()
    res.invoke { t -> trees.add(t) }
    return trees
}

