package org.parser.neo4j

import org.junit.jupiter.api.Assertions.*
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.parser.combinators.graph.GraphCombinators
import kotlin.test.assertContains
import org.parser.neo4j.DefaultNeo4jCombinators

class SimpleTest : BaseDefaultNeo4jTest() {

    override fun fillDb(db: GraphDatabaseService) {
        db.beginTx().let { tx ->
            val a = tx.createNode(Label.label("A"))
            a.setProperty("key", "value")
            val b = tx.createNode(Label.label("B"))
            val e = a.createRelationshipTo(b) { "e" }
            e.setProperty("key", "value")

            tx.commit()
        }

    }

    override fun testGraph(gr: Neo4jGraph<DefaultNeo4jNode, DefaultNeo4jEdge>) {
        val vertexes = gr.getVertexes()
        val aVertex = vertexes.firstOrNull { it.labels.contains("A") }
        kotlin.test.assertNotNull(aVertex)
        assertContains(aVertex.properties, "key")
        assertEquals("value", aVertex.properties["key"])
        val bVertex = vertexes.firstOrNull { it.labels.contains("B") }
        kotlin.test.assertNotNull(bVertex)
        val edges = gr.getOutgoingEdges(aVertex)
        kotlin.test.assertNotNull(edges)
        assertEquals(1, edges.size)
        assertEquals("e", edges[0].label)
        assertContains(edges[0].properties, "key")
        assertEquals("value", edges[0].properties["key"])
        println(vertexes)
        println(edges)
    }


}