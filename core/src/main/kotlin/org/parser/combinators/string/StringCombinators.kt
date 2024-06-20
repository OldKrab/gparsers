package org.parser.combinators.string

import org.parser.combinators.Parser
import org.parser.combinators.ParserResult
import org.parser.sppf.NonPackedNode
import org.parser.sppf.SPPFStorage

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
    get() = object : StringParser<String>(this) {
        override fun parse(
            sppf: SPPFStorage,
            inS: StringPos
        ): ParserResult<NonPackedNode<StringPos, StringPos, String>> {
            val str = this@p
            return if (inS.str.startsWith(str, inS.pos))
                ParserResult.success(sppf.getTerminalNode(inS, inS.move(str.length), str))
            else
                ParserResult.failure()
        }
    }

val Regex.p: StringParser<String>
    get() = object : StringParser<String>("\"$this\"") {
        override fun parse(
            sppf: SPPFStorage,
            inS: StringPos
        ): ParserResult<NonPackedNode<StringPos, StringPos, String>> {
            val str = this@p
            val matchRes = str.matchAt(inS.str, inS.pos)
            return if (matchRes != null) {
                val match = matchRes.value
                ParserResult.success(sppf.getTerminalNode(inS, inS.move(match.length), match))
            } else
                ParserResult.failure()
        }
    }

