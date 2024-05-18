package org.parser.combinators

import org.parser.sppf.SPPFStorage
import org.parser.sppf.NonPackedNode
import kotlin.reflect.KProperty



/**
 * The [Parser] used to parse any environment by defining [parse] function.
 *
 * @property parse Parses [InS] state using [SPPFStorage] to store SPPF nodes. Returns [ParserResult] with [NonPackedNode] nodes.
 */
class Parser<InS, OutS, out R> private constructor(
    private val parseFun: (SPPFStorage, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>,
    override var view: String
) : BaseParser<InS, OutS, R> {

    override fun parse(sppf: SPPFStorage, inS: InS): ParserResult<NonPackedNode<InS, OutS, R>> {
        return parseFun(sppf, inS)
    }

    override fun toString(): String {
        return view
    }

    companion object {
        /** Returns parser which results will be memoized for any input state. */
        internal fun <InS, OutS, R> memo(
            name: String,
            inner: (SPPFStorage, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>
        ): Parser<InS, OutS, R> {
            val table = HashMap<InS, ParserResult<NonPackedNode<InS, OutS, R>>>()
            val res: Parser<InS, OutS, R> = Parser(
                { sppf, inS -> table.computeIfAbsent(inS) { ParserResult.memoResult { inner(sppf, inS) } } },
                name
            )
            return res
        }

        /** Returns new parser. */
        internal fun <InS, OutS, R> new(
            name: String,
            inner: (SPPFStorage, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>
        ): Parser<InS, OutS, R> {
            val res: Parser<InS, OutS, R> = Parser(inner, name)
            return res
        }
    }
}



