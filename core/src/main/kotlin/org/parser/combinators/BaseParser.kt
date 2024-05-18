package org.parser.combinators

import org.parser.sppf.NonPackedNode
import org.parser.sppf.SPPFStorage
import kotlin.reflect.KProperty

/**
 * The [BaseParser] used to parse any environment by defining [parse] function.
 *
 * @property parse Parses [InS] state using [SPPFStorage] to store SPPF nodes. Returns [ParserResult] with [NonPackedNode] nodes.
 */
abstract class BaseParser<InS, OutS, out R>(open val isMemoized: Boolean, open var view: String) {
    abstract fun parse(sppf: SPPFStorage, inS: InS): ParserResult<NonPackedNode<InS, OutS, R>>


    /** Applies parser to [inState] state. */
    fun parseState(inState: InS): List<NonPackedNode<InS, OutS, R>> {
        val sppf = SPPFStorage()
        val res = this.parse(sppf, inState)
        val trees = res.getResults()
        return trees
    }

    open operator fun getValue(r: Any?, property: KProperty<*>): BaseParser<InS, OutS, R> {
        return this
    }

    open operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): BaseParser<InS, OutS, R> {
        this.view = property.name
        return this
    }

    override fun toString(): String {
        return view
    }
}