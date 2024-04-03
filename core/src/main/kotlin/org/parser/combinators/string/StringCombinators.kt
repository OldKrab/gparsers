package org.parser.combinators.string

import org.parser.combinators.Combinators
import org.parser.combinators.Parser
import org.parser.combinators.ParserResult
import org.parser.combinators.applyParser
import org.parser.sppf.node.NonPackedNode

typealias StringParser<R> = Parser<String, StringPos, StringPos, R>

data class StringPos(val pos: Int) {
    fun move(d: Int) = StringPos(pos + d)
    override fun toString(): String {
        return pos.toString()
    }
}

fun <R> String.applyParser(parser: StringParser<R>, count: Int = -1): List<NonPackedNode<StringPos, StringPos, R>> {
    return applyParser(this, parser, StringPos(0), count)
}

val String.p: StringParser<String>
    get() = Parser.memo("\"$this\"") { str, sppf, i ->
        if (str.startsWith(this, i.pos))
            ParserResult.success(sppf.getTerminalNode(i, i.move(this.length), this))
        else
            ParserResult.failure()
    }

object StringCombinators : Combinators<String>
