package org.graphparser

import org.junit.jupiter.api.Assertions.assertEquals
import org.parser.combinators.string.StringParser

import org.parser.combinators.string.applyParser
import org.parser.combinators.string.p
import org.junit.jupiter.api.Test
import org.parser.combinators.*
import org.parser.combinators.string.StringPos


class StringParserTests :ParserTests() {

    @Test
    fun simple() {
        val S = "a".p
        val str = "a"
        val results = str.applyParser(S).filter { it.rightState.pos == str.length }
        assertEquals(1, results.size)
        assertEquals(setOf("a"), results[0].getResults().toSet())
    }

    @Test
    fun sameTerminalParserWithDifferentAction() {
        val S = "a".p using { _ -> 42 } or ("a".p using {_ -> 24})
        val str = "a"
        val nodes = str.applyParser(S)
        assertEquals(1, nodes.size)
        assertEquals(setOf(42, 24), nodes[0].getResults().toSet())
    }

    @Test
    fun sameEpsilonParserWithDifferentAction() {
        val S = eps<StringPos>() using { _ -> 42 } or (eps<StringPos>() using { _ -> 24})
        val str = "a"
        val nodes = str.applyParser(S)
        assertEquals(1, nodes.size)
        assertEquals(setOf(42, 24), nodes[0].getResults().toSet())
    }

    @Test
    fun simpleAmbiguous() {
        val str = "aa"

        val S: StringParser<String> = rule(
            ("a".p seq "a".p) using { _, _ -> "[a][a]" },
            "aa".p using { "[aa]" }
        )
        val nodes = str.applyParser(S)
        saveDotsToFolder(nodes, "simpleAmbiguous")

        val results = nodes.map { it.getResults() }
        assertEquals(1, results.size)
        assertEquals(setOf("[aa]", "[a][a]"), results[0].toSet())
    }

    @Test
    fun ambiguousWithFix() {
        val str = "aaaaaa"
        val a = "a".p
        val S: StringParser<String> = fix("S") { S ->
            (a seqr S seql a).many using { s -> s.joinToString("") { "[a${it}a]" } }
        }

        val nodes = str.applyParser(S)
        saveDotsToFolder(nodes, "ambiguous")

        val results = nodes.filter { it.rightState.pos == str.length }.map { it.getResults() }
        assertEquals(1, results.size)
        assertEquals(
            setOf("[aa][aa][aa]", "[aa][a[aa]a]", "[a[aa]a][aa]", "[a[aa][aa]a]", "[a[a[aa]a]a]"),
            results[0].toSet()
        )
    }

    @Test
    fun ambiguousWithRule() {
        val S: StringParser<String> = fix("S") { S ->
            rule(
                ("a".p seqr S seql "a".p seq S) using { s1, s2 -> "[a${s1}a]$s2" },
                "".p
            )
        }

        val str = "aaaa"
        val nodes = str.applyParser(S)
        saveDotsToFolder(nodes, "ambiguous2")
        val results = nodes.filter { it.rightState.pos == str.length }.map { it.getResults() }
        assertEquals(1, results.size)
        assertEquals(setOf("[aa][aa]", "[a[aa]a]"), results[0].toSet())
    }

    @Test
    fun brackets() {
        val S: StringParser<String> = fix("S") { S ->
            rule(
                ("[".p seqr S seql "]".p seq S) using { s1, s2 -> "[$s1]$s2" },
                "".p
            )
        }

        val str = "[][[]][[][]]"
        val nodes = str.applyParser(S)
        saveDotsToFolder(nodes, "brackets")
        val results = nodes.filter { it.rightState.pos == str.length }.map { it.getResults() }
        assertEquals(1, results.size)
        assertEquals(setOf("[][[]][[][]]"), results[0].toSet())
    }

    @Test
    fun leftRec() {
        val a = "a".p
        val S = fix("S") { S ->
            rule(
                S seq a using { a, b -> "$a$b" },
                a
            )
        }

        val str = "aaaa"
        val nodes = str.applyParser(S)

        saveDotsToFolder(nodes, "leftRec")
        val results = nodes.map { it.getResults().toList() }.onEach { assertEquals(1, it.size) }.map { it[0] }
        assertEquals(setOf("a", "aa", "aaa", "aaaa"), results.toSet())
    }

    @Test
    fun infinite() {
        val p = ("".p).many

        val str = "aaaa"
        val nodes = str.applyParser(p)
        saveDotsToFolder(nodes, "infinite")
    }
}