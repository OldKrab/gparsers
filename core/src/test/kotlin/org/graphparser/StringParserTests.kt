package org.graphparser

import org.graphparser.StringParsers.fix
import org.graphparser.StringParsers.many
import org.graphparser.StringParsers.seq
import org.graphparser.StringParsers.seql
import org.graphparser.StringParsers.seqr
import org.graphparser.StringParsers.rule
import org.graphparser.StringParsers.using
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class StringParserTests {


    @Test
    fun ambiguous() {
        val a = "a".p
        val ambiguous: StringParser<String> = fix { S ->
            (a seqr S seql a).many using { s -> s.joinToString("") { "[a${it}a]" } }
        }

        val str = "aaaaaa"
        val results = str.applyParser(ambiguous)
            .filter { it.first.pos == str.length }
            .map { it.second }.toSet()
        assertEquals(setOf("[aa][aa][aa]", "[aa][a[aa]a]", "[a[aa]a][aa]", "[a[aa][aa]a]", "[a[a[aa]a]a]"), results)
    }

    @Test
    fun brackets() {
        val brackets: StringParser<String> = fix { S ->
            rule(
                ("[".p seqr S seql "]".p seq S) using { s1, s2 -> "[$s1]$s2" },
                "".p
            )
        }

        val str = "[][[]][[][]]"
        val results = str.applyParser(brackets).map { it.second }.toSet()
        assertEquals(setOf("[][[]][[][]]", "[][[]]", "[]", ""), results)
    }

    @Test
    fun leftRec() {
        val a = "a".p
        val p = fix { S ->
            rule(
                S seq a using { a, b -> "$a$b" },
                a
            )
        }

        val str = "aaaa"
        val results = str.applyParser(p).map { it.second }.toSet()
        assertEquals(setOf("a", "aa", "aaa", "aaaa"), results)
    }

    @Test
    fun infinite() {
        val p = ("".p).many

        val str = "aaaa"
        val results = str.applyParser(p, 3).map { it.second.toList() }.toSet()
        assertEquals(setOf(listOf(), listOf(""), listOf("", "")), results)

    }
}