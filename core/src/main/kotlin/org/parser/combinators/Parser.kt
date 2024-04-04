package org.parser.combinators

import org.parser.sppf.SPPF
import org.parser.sppf.NonPackedNode
import java.nio.file.Path

interface BaseParser
class Parser<InS, OutS, R> private constructor(
    var inner: (SPPF, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>,
    var name: String
) : BaseParser {
    fun parse(sppf: SPPF, input: InS): ParserResult<NonPackedNode<InS, OutS, R>> =
        inner(sppf, input)

    override fun toString(): String {
        return name
    }


    companion object {

        fun <InS, OutS, R> memo(name: String, inner: (SPPF, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>): Parser<InS, OutS, R> {
            val table = HashMap<InS, ParserResult<NonPackedNode<InS, OutS, R>>>()
            val res: Parser<InS, OutS, R> = Parser(
                { sppf, inS -> table.computeIfAbsent(inS) { memoResult { inner(sppf, inS) } } },
                name
            )
            return res
        }

        private fun <T> memoResult(res: () -> ParserResult<T>): ParserResult<T> {
            val results = ArrayList<T>()
            val continuations = ArrayList<Continuation<T>>()
            return ParserResult { k ->
                if (continuations.isEmpty()) {
                    continuations.add(k)
                    val p = res()
                    p.invoke { t ->
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

fun <I, O, R> applyParser(parser: Parser<I, O, R>, inState: I): List<NonPackedNode<I, O, R>> {
    val sppf = SPPF()
    val res = parser.parse(sppf, inState)
    val trees = res.getResults()
    sppf.convertAllToDot(Path.of("/tmp/boob.dot"))
    return trees
}

