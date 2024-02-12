package org.example

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StringParserTests {



    @Test
    fun ambiguous() {
        val a = "a".l
        val ambiguous: StringParser<String> = fix { S ->
            (a seqr S seql a).many using { s -> s.joinToString(separator = "") { "[a${it}a]" } }
        }

        val str2 = "aaaaaa"
        ambiguous.parse(StringPos(str2)).forEach { println(it) }

    }

    @Test
    fun brackets() {
        val brackets: StringParser<String> = fix { brackets ->
            ("[".l seqr brackets seql "]".l seq brackets) using { s1, s2 -> "[$s1]$s2" } or
                    "".l
        }

        val str = "[][[][]]"
        brackets.parse(StringPos(str)).forEach { println(it) }
    }

    @Test
    fun leftRec() {
        assertThrows<Throwable> {
            val a = "a".l
            val p = fix { S ->
                (S seq a using { a, b -> "${a}${b}" }) or "".l
            }

            val str2 = "aaaaaa"
            p.parse(StringPos(str2)).forEach { println(it) }
        }

    }
}