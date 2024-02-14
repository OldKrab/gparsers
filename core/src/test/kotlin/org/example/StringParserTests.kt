package org.example

import org.example.StringParsers.fix
import org.example.StringParsers.many
import org.example.StringParsers.seq
import org.example.StringParsers.seql
import org.example.StringParsers.seqr
import org.example.StringParsers.or
import org.example.StringParsers.using
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class StringParserTests {


    @Test
    fun ambiguous() {
        val a = "a".l
        val ambiguous: StringParser<String> = fix { S ->
            (a seqr S seql a).many using { s -> s.joinToString(separator = "") { "[a${it}a]" } }
        }

        val str = "aaaaaa"
        ambiguous.parse(str, StringPos(0)).forEach { println(it) }

    }

    @Test
    fun brackets() {
        val brackets: StringParser<String> = fix { brackets ->
            ("[".l seqr brackets seql "]".l seq brackets) using { s1, s2 -> "[$s1]$s2" } or
                    "".l
        }

        val str = "[][[][]]"
        brackets.parse(str, StringPos(0)).forEach { println(it) }
    }

    @Test
    fun leftRec() {
        assertThrows<Throwable> {
            val a = "a".l
            val p = fix { S ->
                (S seq a using { a, b -> "${a}${b}" }) or "".l
            }

            val str = "aaaaaa"
            p.parse(str, StringPos(0)).forEach { println(it) }
        }

    }
}