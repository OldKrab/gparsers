package org.parser.combinators

import org.parser.sppf.SPPFStorage
import org.parser.sppf.NonPackedNode



/**
 * The [Parser] implements [BaseParser] with parser function [parseFun]
 */
class Parser<InS, OutS, out R> private constructor(
    private val parseFun: (SPPFStorage, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>,
    view: String,
    isMemoized: Boolean
) : BaseParser<InS, OutS, R>(isMemoized, view) {

    override fun parse(sppf: SPPFStorage, inS: InS): ParserResult<NonPackedNode<InS, OutS, R>> {
        return parseFun(sppf, inS)
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
                name,
                true
            )
            return res
        }

        /** Returns new parser. */
        internal fun <InS, OutS, R> new(
            name: String,
            inner: (SPPFStorage, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>
        ): Parser<InS, OutS, R> {
            val res: Parser<InS, OutS, R> = Parser(inner, name, false)
            return res
        }

        /** Returns new parser that will be memoized if [isMemoized] set to true. */
        internal fun <InS, OutS, R> new(
            name: String,
            isMemoized: Boolean,
            inner: (SPPFStorage, InS) -> ParserResult<NonPackedNode<InS, OutS, R>>,
        ): Parser<InS, OutS, R> {
            if(isMemoized) return memo(name, inner)
            return new(name,  inner)
        }
    }
}





