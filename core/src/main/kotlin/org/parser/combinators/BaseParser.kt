package org.parser.combinators

import org.parser.sppf.NonPackedNode
import org.parser.sppf.SPPFStorage
import kotlin.reflect.KProperty

interface BaseParser<InS, OutS, out R> {
    fun parse(sppf: SPPFStorage, inS: InS): ParserResult<NonPackedNode<InS, OutS, R>>
    var view: String

    /** Applies parser to [inState] state. */
    fun parseState(inState: InS): List<NonPackedNode<InS, OutS, R>> {
        val sppf = SPPFStorage()
        val res = this.parse(sppf, inState)
        val trees = res.getResults()
        return trees
    }

    operator fun getValue(r: Any?, property: KProperty<*>): BaseParser<InS, OutS, R> {
        return this
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): BaseParser<InS, OutS, R> {
        this.view = property.name
        return this
    }

}