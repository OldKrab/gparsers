package org.parser.combinators

import org.parser.sppf.SPPFStorage
import org.parser.sppf.NonPackedNode
import kotlin.reflect.KProperty

interface BaseParser

/**
 * The [Parser] used to parse any environment by defining [parse] function.
 *
 * @property parse Parses [InS] state using [SPPFStorage] to store SPPF nodes. Returns [ParserResult] with [NonPackedNode] nodes.
 */
class Parser<InS, OutS, R> private constructor(
    val parse: (SPPFStorage, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>,
    var view: String
) : BaseParser {

    override fun toString(): String {
        return view
    }

    /** Applies parser to [inState] state. */
    fun parseState(inState: InS): List<NonPackedNode<InS, OutS, R>> {
        val sppf = SPPFStorage()
        val res = this.parse(sppf, inState)
        val trees = res.getResults()
        return trees
    }

    operator fun getValue(r: Any?, property: KProperty<*>): Parser<InS, OutS, R> {
        val name = property.name
        return fix(name) { q ->
            Parser.memo(name) { sppf, inS ->
                this.parse(sppf, inS).map { t -> sppf.getNonTerminalNode(name, q, t) }
            }
        }
    }

    companion object {
        /** Returns parser which results will be memoized for any input state. */
        internal fun <InS, OutS, R> memo(name: String, inner: (SPPFStorage, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>): Parser<InS, OutS, R> {
            val table = HashMap<InS, ParserResult<NonPackedNode<InS, OutS, R>>>()
            val res: Parser<InS, OutS, R> = Parser(
                { sppf, inS -> table.computeIfAbsent(inS) { ParserResult.memoResult { inner(sppf, inS) } } },
                name
            )
            return res
        }
    }
}



