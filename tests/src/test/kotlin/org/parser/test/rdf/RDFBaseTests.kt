package org.parser.test.rdf

import org.apache.jena.rdf.model.ModelFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.neo4j.graphdb.GraphDatabaseService
import org.parser.combinators.*
import org.parser.combinators.graph.VertexState
import org.parser.neo4j.DefaultNeo4jCombinators.throughInE
import org.parser.neo4j.DefaultNeo4jCombinators.throughOutE
import org.parser.neo4j.DefaultNeo4jGraph
import org.parser.neo4j.DefaultNeo4jVertexState
import java.net.URI
import java.nio.file.Path
import java.util.*
import kotlin.test.assertEquals

class RDFBaseTests {

    fun createNeo4jDb(file: String): DatabaseManagementService {
        val dbPath = Path.of("temp_dbs/gparsers/$file")
        val managementService = DatabaseManagementServiceBuilder(dbPath).build();
        return managementService
    }

    data class Edge(val from: Int, val label: String, val to: Int)

    private fun getGraph(file: String, db: GraphDatabaseService): DefaultNeo4jGraph {
        fun getTriples(file: String): List<Triple<String, String, String>> {
            val inputStream = this.javaClass.getResourceAsStream("/rdf/$file")
                ?: throw RuntimeException("Can't find rdf resource $file")
            val model = ModelFactory.createDefaultModel()
            model.read(inputStream, null)
            return model
                .listStatements()
                .toList()
                .map { stmt ->
                    Triple(
                        stmt.getObject().toString(),
                        stmt.getPredicate().toString(),
                        stmt.getSubject().toString()
                    )
                }
        }

        fun triplesToEdges(triples: List<Triple<String, String, String>>): Pair<List<Edge>, Int> {
            val nodes: Map<String, Int> = triples
                .asSequence()
                .flatMap { (f, _, t) -> sequenceOf(f, t) }
                .toSet().sorted().withIndex()
                .associate { it.value to it.index }

            val edges = triples.map { (f, l, t) ->
                val from = nodes[f]!!
                val to = nodes[t]!!
                val label = URI(l).fragment?.lowercase(Locale.getDefault())
                Edge(from, label ?: "noLabel", to)
            }
            return Pair(edges, nodes.size)
        }

        fun edgesToNeo4jGraph(db: GraphDatabaseService, edges: List<Edge>, nodesCount: Int): DefaultNeo4jGraph {
            val tx = db.beginTx()
            if (tx.allRelationships.count() == edges.size) {
                println("db already filled")
                return DefaultNeo4jGraph(db)
            }
            println("creating db")
            val nodes = List(nodesCount) { tx.createNode() }
            edges.forEach { e ->
                nodes[e.from].createRelationshipTo(nodes[e.to]) { e.label }
            }
            tx.commit()
            return DefaultNeo4jGraph(db)
        }

        val tx = db.beginTx()
        if (tx.allRelationships.count() > 0) {
            println("db already filled")
            return DefaultNeo4jGraph(db)
        }
        val triples = getTriples(file)
        val (edges, nodesCount) = triplesToEdges(triples)
        val graph = edgesToNeo4jGraph(db, edges, nodesCount)
        return graph
    }

    fun <T> parse(
        graph: DefaultNeo4jGraph,
        grammar: Parser<DefaultNeo4jVertexState, DefaultNeo4jVertexState, T>
    ): Int {
        val nodes = graph.getVertexes()
        var size = 0
        var cnt = 0
        for (node in nodes) {
            val nodes = grammar.parseState(VertexState(graph, node))
            val cur = nodes.size
            size += cur
            cnt += 1
//            if(cur != 0){
//                println("$cnt -> $cur")
//            }
//            if (cnt % 100000 == 0)
//                println("Parsed $cnt nodes")
        }
        return size
    }


    fun firstGrammar(): Parser<DefaultNeo4jVertexState, DefaultNeo4jVertexState, Any> {
        val subclassof1 = throughInE { it.label == "subclassof" }
        val subclassof = throughOutE { it.label == "subclassof" }
        val type1 = throughInE { it.label == "type" }
        val type = throughOutE { it.label == "type" }
        val S = LateInitParser<DefaultNeo4jVertexState, DefaultNeo4jVertexState, Any>()
        S.p =
            (subclassof1 seq (S or eps()) seq subclassof) or
                    (type1 seq (S or eps()) seq type)

        return S
    }

    fun secondGrammar(): Parser<DefaultNeo4jVertexState, DefaultNeo4jVertexState, Any> {
        val subclassof1 = throughInE { it.label == "subclassof" }
        val subclassof = throughOutE { it.label == "subclassof" }

        val S = LateInitParser<DefaultNeo4jVertexState, DefaultNeo4jVertexState, Any>()
        S.p = (subclassof1 seq (S or eps()) seq subclassof)

        return ((S or eps()) seq subclassof)
    }


    @ParameterizedTest
    @MethodSource("getFiles")
    fun query(file: String, firstQueryExpected: Int, secondQueryExpected: Int) {
        val neo4j = createNeo4jDb(file)
        val graph = getGraph(file, neo4j.database(DEFAULT_DATABASE_NAME))
        assertEquals(firstQueryExpected, parse(graph, firstGrammar()))
        assertEquals(secondQueryExpected, parse(graph, secondGrammar()))
    }


    companion object {
        @JvmStatic
        fun getFiles(): List<Arguments> {
            return listOf(
                arguments("atom-primitive.owl", 15454, 122),
                arguments("biomedical-mesure-primitive.owl", 15156, 2871),
                arguments("foaf.rdf", 4118, 10),
                arguments("generations.owl", 2164, 0),
                arguments("people_pets.rdf", 9472, 37),
                arguments("pizza.owl", 56195, 1262),
                arguments("skos.rdf", 810, 1),
                arguments("travel.owl", 2499, 63),
                arguments("univ-bench.owl", 2540, 81),
                arguments("wine.rdf", 66572, 133)
            )
        }
    }
}