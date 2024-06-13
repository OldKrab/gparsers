package org.parser

import org.parser.sppf.Visualizer
import org.parser.sppf.Node
import java.nio.file.Path
import kotlin.io.path.createDirectories

object ParserTests {
     fun saveDotsToFolder(nodes: List<Node>, dirName: String) {
        val dir = Path.of(System.getProperty("java.io.tmpdir")).resolve(dirName).createDirectories()
        for (i in nodes.indices) {
            println(nodes[i])
            Visualizer().toDotFile(nodes[i], dir.resolve("$i.dot"))
        }
        println("Look dots in '$dir'")
    }

}