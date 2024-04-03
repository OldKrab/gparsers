package org.graphparser

import org.parser.combinators.string.StringParser
import org.parser.combinators.string.StringCombinators.fix
import org.parser.combinators.string.StringCombinators.many
import org.parser.combinators.string.StringCombinators.seql
import org.parser.combinators.string.StringCombinators.seqr
import org.parser.combinators.string.StringCombinators.using
import org.parser.combinators.string.applyParser
import org.parser.combinators.string.p
import org.junit.jupiter.api.Test
import org.parser.combinators.string.StringCombinators.or
import org.parser.combinators.string.StringCombinators.rule
import org.parser.combinators.string.StringCombinators.seq
import org.parser.sppf.Visualizer
import java.nio.file.Path
import kotlin.io.path.createDirectories


class StringParserTests {
    @Test
    fun simple() {
        val S: StringParser<String> = fix("S") { S -> ("a".p seq S) using { a, s -> a + s } or "".p }

        val str = "a"
        val results = str.applyParser(S)
        results[0].toString()
//        val results = str.applyParser(ambiguous)
//            .filter { it.first.pos == str.length }
//            .map { it.second }.toSet()
//        assertEquals(setOf("[aa][aa][aa]", "[aa][a[aa]a]", "[a[aa]a][aa]", "[a[aa][aa]a]", "[a[a[aa]a]a]"), results)
    }

    @Test
    fun ambiguous() {
        val a = "a".p
        val S: StringParser<String> = fix("S") { S ->
            (a seqr S seql a).many using { s -> s.joinToString("") { "[a${it}a]" } }
        }

        val str = "aaaaaa"
        val results = str.applyParser(S)


        val dir = Path.of(System.getProperty("java.io.tmpdir")).resolve("ambigious").createDirectories()
        for (i in results.indices) {
            Visualizer().toDotFile(results[i], dir.resolve("$i.dot"))
        }
        println("Look images in '$dir'")
        return
//        val results = str.applyParser(ambiguous)
//            .filter { it.first.pos == str.length }
//            .map { it.second }.toSet()
//        assertEquals(setOf("[aa][aa][aa]", "[aa][a[aa]a]", "[a[aa]a][aa]", "[a[aa][aa]a]", "[a[a[aa]a]a]"), results)
    }

    //
    @Test
    fun brackets() {
        val S: StringParser<String> = fix("S") { S ->
            rule(
                ("[".p seqr S seql "]".p seq S) using { s1, s2 -> "[$s1]$s2" },
                "".p
            )
        }

        val str = "[][[]][[][]]"
        val results = str.applyParser(S)
        val dir = Path.of(System.getProperty("java.io.tmpdir")).resolve("brackets").createDirectories()
        for (i in results.indices) {
            Visualizer().toDotFile(results[i], dir.resolve("$i.dot"))
        }
        println("Look images in '$dir'")
    }
//
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
        val results = str.applyParser(S)
        val dir = Path.of(System.getProperty("java.io.tmpdir")).resolve("leftRec").createDirectories()
        for (i in results.indices) {
            Visualizer().toDotFile(results[i], dir.resolve("$i.dot"))
        }
        println("Look images in '$dir'")
        //assertEquals(setOf("a", "aa", "aaa", "aaaa"), results)
    }
//
//    @Test
//    fun infinite() {
//        val p = ("".p).many
//
//        val str = "aaaa"
//        val results = str.applyParser(p, 3).map { it.second.toList() }.toSet()
//        assertEquals(setOf(listOf(), listOf(""), listOf("", "")), results)
//
//    }
}