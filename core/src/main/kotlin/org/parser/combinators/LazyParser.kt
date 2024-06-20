package org.parser.combinators

import org.parser.sppf.NonPackedNode
import org.parser.sppf.SPPFStorage


/** [LazyParser] represent parser with lazy evaluation. Useful when need create recursive parser.
 *
 * In [getP] definition you can use this as defining parser */
class LazyParser<InS, OutS, R>(private val getP: Parser<InS, OutS, R>.() -> Parser<InS, OutS, R>) :
    Parser<InS, OutS, R>("lazy") {
    lateinit var p: Parser<InS, OutS, R>

    override var view: String = "lazy"
        get() = if (field != "lazy" || !::p.isInitialized) field else p.view
        set(value) {
            if (::p.isInitialized)
                p.view = value
            field = value
        }

    override fun parse(sppf: SPPFStorage, inS: InS): ParserResult<NonPackedNode<InS, OutS, R>> {
        if (!this::p.isInitialized)
            p = this.getP()
        return p.parse(sppf, inS)
    }
}