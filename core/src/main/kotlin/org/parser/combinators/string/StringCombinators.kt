package org.parser.combinators.string

import org.parser.combinators.Parser
import org.parser.combinators.ParserResult
import org.parser.combinators.applyParser
import org.parser.sppf.NonPackedNode

typealias StringParser<R> = Parser<StringPos, StringPos, R>

data class StringPos(val str: String, val pos: Int) {
    fun move(d: Int) = StringPos(str, pos + d)
    override fun toString(): String {
        return pos.toString()
    }
}

fun <R> String.applyParser(parser: StringParser<R>): List<NonPackedNode<StringPos, StringPos, R>> {
    return applyParser(parser, StringPos(this, 0))
}

val String.p: StringParser<String>
    get() = Parser.memo("\"$this\"") { sppf, i ->
        if (i.str.startsWith(this, i.pos))
            ParserResult.success(sppf.getTerminalNode(i, i.move(this.length), this))
        else
            ParserResult.failure()
    }

