package org.parser.combinators

import org.parser.sppf.NonPackedNode
import org.parser.sppf.SPPFStorage
import kotlin.reflect.KProperty

/** [LateInitParser] represent parser with late initialization. Useful when need create mutual recursive parsers. */
class LateInitParser<InS, OutS, R> : Parser<InS, OutS, R>("lateinit") {
    private lateinit var p: Parser<InS, OutS, R>

    override var view: String = "lateinit"
        get() = if (field != "lateinit" || !::p.isInitialized) field else p.view
        set(value) {
            if (::p.isInitialized)
                p.view = value
            field = value
        }

    fun init(p: Parser<InS, OutS, R>): Unit{
        this.p = p
    }

    override fun parse(sppf: SPPFStorage, inS: InS): ParserResult<NonPackedNode<InS, OutS, R>> {
        if (!this::p.isInitialized)
            throw UninitializedPropertyAccessException("Parser not initialized")
        return p.parse(sppf, inS)
    }

    override operator fun getValue(r: Any?, property: KProperty<*>): LateInitParser<InS, OutS, R> {
        return this
    }

    override operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): LateInitParser<InS, OutS, R> {
        this.view = property.name
        return this
    }
}
