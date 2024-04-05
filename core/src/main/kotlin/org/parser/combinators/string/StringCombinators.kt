package org.parser.combinators.string

import org.parser.combinators.Parser
import org.parser.combinators.ParserResult
import org.parser.sppf.NonPackedNode

/** State of string parsing. Contains string and position in it. */
data class StringPos(val str: String, val pos: Int) {
    fun move(d: Int) = StringPos(str, pos + d)
    override fun toString(): String {
        return pos.toString()
    }
}

typealias StringParser<R> = Parser<StringPos, StringPos, R>

/**
 * Applies parser to the string from the beginning.
 * @return list of [NonPackedNode]. */
fun <R> String.applyParser(parser: StringParser<R>): List<NonPackedNode<StringPos, StringPos, R>> {
    return parser.parseState(StringPos(this, 0))
}

/** Returns parser that parses this string. */
val String.p: StringParser<String>
    get() = Parser.memo("\"$this\"") { sppf, i ->
        if (i.str.startsWith(this, i.pos))
            ParserResult.success(sppf.getTerminalNode(i, i.move(this.length), this))
        else
            ParserResult.failure()
    }

val Regex.p: StringParser<String>
    get() = Parser.memo("\"$this\"") { sppf, i ->
        val matchRes = this.matchAt(i.str, i.pos)
        if (matchRes != null) {
            val match = matchRes.value
            ParserResult.success(sppf.getTerminalNode(i, i.move(match.length), match))
        } else
            ParserResult.failure()
    }