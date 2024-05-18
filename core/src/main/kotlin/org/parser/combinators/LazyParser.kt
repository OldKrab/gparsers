package org.parser.combinators

import org.parser.sppf.NonPackedNode
import org.parser.sppf.SPPFStorage
import kotlin.reflect.KProperty

class LazyParser<InS, OutS, R> : BaseParser<InS, OutS, R> {
    lateinit var p: BaseParser<InS, OutS, R>
    override var view: String = "lazy"

    override fun parse(sppf: SPPFStorage, inS: InS): ParserResult<NonPackedNode<InS, OutS, R>> {
        if (!this::p.isInitialized)
            throw UninitializedPropertyAccessException("Parser not initialized")
        return p.parse(sppf, inS)
    }
}
